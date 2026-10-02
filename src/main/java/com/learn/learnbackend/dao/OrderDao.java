package com.learn.learnbackend.dao;

import com.learn.learnbackend.model.CustomerOrder;
import com.learn.learnbackend.model.OrderItem;
import com.learn.learnbackend.model.OrderSort;
import com.learn.learnbackend.util.JdbcUtil;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 订单和订单明细的数据访问层。 */
@Repository
public class OrderDao {
    private final JdbcUtil jdbc;
    public OrderDao(JdbcUtil jdbc) { this.jdbc = jdbc; }

    /** 在调用方事务内创建订单头并返回自增编号。 */
    public long insertOrder(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO orders(total_price) VALUES (0)", Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("数据库未返回订单编号"); return keys.getLong(1); }
        }
    }

    /** 插入带商品名称和成交价格快照的订单明细。 */
    public void insertItem(Connection c, long orderId, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items(order_id, product_id, product_name_snapshot, unit_price, quantity) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId); ps.setLong(2, item.productId()); ps.setString(3, item.productName()); ps.setBigDecimal(4, item.unitPrice()); ps.setInt(5, item.quantity()); ps.executeUpdate();
        }
    }

    /** 更新订单汇总金额。 */
    public void updateTotal(Connection c, long orderId, BigDecimal total) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE orders SET total_price = ? WHERE id = ? AND deleted_at IS NULL")) {
            ps.setBigDecimal(1, total); ps.setLong(2, orderId); ps.executeUpdate();
        }
    }

    /** 查询订单头和对应的全部商品明细。 */
    public Optional<CustomerOrder> findById(long id) {
        try (Connection c = jdbc.getConnection()) { return findById(c, id); }
        catch (SQLException e) { throw failure("查询订单失败", e); }
    }

    /** 使用已有连接查询订单，支持在事务中读取刚创建或刚更新的订单。 */
    public Optional<CustomerOrder> findById(Connection c, long id) throws SQLException {
        String sql = "SELECT id, ordered_at, total_price, deleted_at FROM orders WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Timestamp timestamp = rs.getTimestamp("ordered_at");
                Timestamp deletedAt = rs.getTimestamp("deleted_at");
                return Optional.of(new CustomerOrder(rs.getLong("id"), timestamp.toLocalDateTime(),
                        rs.getBigDecimal("total_price"), deletedAt == null ? null : deletedAt.toLocalDateTime(), findItems(c, id)));
            }
        }
    }

    /** 按白名单的下单时间或总价排序查询订单摘要。 */
    public List<CustomerOrder> findAll(OrderSort sort) {
        String sql = "SELECT id, ordered_at, total_price, deleted_at FROM orders ORDER BY " + sort.sql() + ", id ASC";
        try (Connection c = jdbc.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<CustomerOrder> result = new ArrayList<>();
            while (rs.next()) {
                Timestamp deletedAt = rs.getTimestamp("deleted_at");
                result.add(new CustomerOrder(rs.getLong("id"), rs.getTimestamp("ordered_at").toLocalDateTime(),
                        rs.getBigDecimal("total_price"), deletedAt == null ? null : deletedAt.toLocalDateTime(), List.of()));
            }
            return result;
        } catch (SQLException e) { throw failure("查询订单列表失败", e); }
    }

    /** 在调用方事务内标记删除，保留订单与明细。 */
    public boolean delete(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE orders SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    /** 清除订单原有明细，供订单更新事务重建明细使用。 */
    public void deleteItems(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM order_items WHERE order_id = ?")) { ps.setLong(1, id); ps.executeUpdate(); }
    }

    /** 批量语义地逐条写入订单明细；调用方保证处于事务内。 */
    public void insertItems(Connection c, long orderId, List<OrderItem> items) throws SQLException {
        for (OrderItem item : items) insertItem(c, orderId, item);
    }

    /** 更新已存在订单的总价。 */
    public void updateTotalById(Connection c, long id, BigDecimal total) throws SQLException { updateTotal(c, id, total); }

    private List<OrderItem> findItems(Connection c, long orderId) throws SQLException {
        String sql = "SELECT product_id, product_name_snapshot, unit_price, quantity FROM order_items WHERE order_id = ? ORDER BY id ASC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderItem> items = new ArrayList<>();
                while (rs.next()) items.add(new OrderItem(rs.getLong("product_id"), rs.getString("product_name_snapshot"), rs.getBigDecimal("unit_price"), rs.getInt("quantity")));
                return items;
            }
        }
    }

    private JdbcUtil.JdbcOperationException failure(String message, SQLException e) { return new JdbcUtil.JdbcOperationException(message, e); }
}
