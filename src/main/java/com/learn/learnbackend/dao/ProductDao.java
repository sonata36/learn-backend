package com.learn.learnbackend.dao;

import com.learn.learnbackend.model.Product;
import com.learn.learnbackend.model.ProductSort;
import com.learn.learnbackend.util.JdbcUtil;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 商品表的数据访问层，所有参数都通过 PreparedStatement 绑定。 */
@Repository
public class ProductDao {
    private final JdbcUtil jdbc;
    public ProductDao(JdbcUtil jdbc) { this.jdbc = jdbc; }

    /** 插入商品并返回数据库生成的编号。 */
    public Product create(String name, java.math.BigDecimal price) {
        String sql = "INSERT INTO products(name, price, active) VALUES (?, ?, TRUE)";
        try (Connection c = jdbc.getConnection(); PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setBigDecimal(2, price); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("数据库未返回商品编号");
                return new Product(keys.getLong(1), name, price, true);
            }
        } catch (SQLException e) { throw failure("创建商品失败", e); }
    }

    /** 根据商品编号查询商品，包括已停用商品。 */
    public Optional<Product> findById(long id) {
        String sql = "SELECT id, name, price, active FROM products WHERE id = ?";
        try (Connection c = jdbc.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw failure("查询商品失败", e); }
    }

    /** 按白名单字段排序查询商品，可选择是否包含停用商品。 */
    public List<Product> findAll(ProductSort sort, boolean includeInactive) {
        String sql = "SELECT id, name, price, active FROM products" + (includeInactive ? "" : " WHERE active = TRUE") + " ORDER BY " + sort.sql() + ", id ASC";
        try (Connection c = jdbc.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Product> products = new ArrayList<>();
            while (rs.next()) products.add(map(rs));
            return products;
        } catch (SQLException e) { throw failure("查询商品列表失败", e); }
    }

    /** 更新仍启用商品的名称与价格。 */
    public boolean update(long id, String name, java.math.BigDecimal price) {
        String sql = "UPDATE products SET name = ?, price = ? WHERE id = ? AND active = TRUE";
        try (Connection c = jdbc.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name); ps.setBigDecimal(2, price); ps.setLong(3, id); return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw failure("更新商品失败", e); }
    }

    /** 软删除：商品被历史订单引用时仍保留其外键及下单快照。 */
    /** 软删除商品，保留被历史订单引用的记录。 */
    public boolean deactivate(long id) {
        String sql = "UPDATE products SET active = FALSE WHERE id = ? AND active = TRUE";
        try (Connection c = jdbc.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id); return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw failure("停用商品失败", e); }
    }

    /** 在调用方事务中锁定并查询可用于新订单的商品。 */
    public Product findActive(Connection c, long id) throws SQLException {
        String sql = "SELECT id, name, price, active FROM products WHERE id = ? AND active = TRUE FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    private Product map(ResultSet rs) throws SQLException { return new Product(rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("price"), rs.getBoolean("active")); }
    private JdbcUtil.JdbcOperationException failure(String message, SQLException e) { return new JdbcUtil.JdbcOperationException(message, e); }
}
