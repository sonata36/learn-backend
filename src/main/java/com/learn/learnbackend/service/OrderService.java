package com.learn.learnbackend.service;

import com.learn.learnbackend.dao.OrderDao;
import com.learn.learnbackend.dao.ProductDao;
import com.learn.learnbackend.model.CustomerOrder;
import com.learn.learnbackend.model.OrderItem;
import com.learn.learnbackend.model.OrderSort;
import com.learn.learnbackend.model.Product;
import com.learn.learnbackend.util.JdbcUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 订单业务层：在单一事务中验证商品、写入快照、计算总价并提交。 */
@Service
public class OrderService {
    private final JdbcUtil jdbc;
    private final ProductDao products;
    private final OrderDao orders;

    public OrderService(JdbcUtil jdbc, ProductDao products, OrderDao orders) {
        this.jdbc = jdbc; this.products = products; this.orders = orders;
    }

    /** 创建订单。Map 的 key 为商品编号、value 为购买数量。 */
    /** 校验购物车并原子创建订单和明细。 */
    public CustomerOrder create(Map<Long, Integer> quantities) {
        return jdbc.inTransaction(connection -> {
            List<OrderItem> items = validatedItems(connection, quantities);
            long orderId = orders.insertOrder(connection);
            orders.insertItems(connection, orderId, items);
            BigDecimal total = totalOf(items);
            orders.updateTotal(connection, orderId, total);
            return orders.findById(connection, orderId).orElseThrow();
        });
    }

    /** 修改订单商品：删除旧明细并重建，整个过程原子提交。 */
    /** 用新商品明细替换订单原明细，并在同一事务内重算总价。 */
    public CustomerOrder update(long orderId, Map<Long, Integer> quantities) {
        return jdbc.inTransaction(connection -> {
            if (orders.findById(connection, orderId).isEmpty()) throw new IllegalArgumentException("订单不存在: " + orderId);
            List<OrderItem> items = validatedItems(connection, quantities);
            orders.deleteItems(connection, orderId);
            orders.insertItems(connection, orderId, items);
            orders.updateTotalById(connection, orderId, totalOf(items));
            return orders.findById(connection, orderId).orElseThrow();
        });
    }

    /** 按编号查询订单及完整明细。 */
    public Optional<CustomerOrder> findById(long id) { return orders.findById(id); }
    /** 按允许的排序选项查询订单列表。 */
    public List<CustomerOrder> findAll(OrderSort sort) { return orders.findAll(sort); }
    /** 删除订单，数据库外键会级联清除其明细。 */
    public void delete(long id) {
        jdbc.inTransaction(connection -> {
            if (!orders.delete(connection, id)) throw new IllegalArgumentException("订单不存在: " + id);
            return null;
        });
    }

    private List<OrderItem> validatedItems(java.sql.Connection connection, Map<Long, Integer> quantities) throws Exception {
        if (quantities == null || quantities.isEmpty()) throw new IllegalArgumentException("订单至少需要一个商品");
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long id = entry.getKey(); Integer quantity = entry.getValue();
            if (id == null || id <= 0) throw new IllegalArgumentException("商品编号必须为正数");
            if (quantity == null || quantity <= 0 || quantity > 10000) throw new IllegalArgumentException("商品数量必须在 1 至 10000 之间");
            Product product = products.findActive(connection, id);
            if (product == null) throw new IllegalArgumentException("商品不存在或已停用: " + id);
            if (product.price() == null || product.price().signum() < 0 || product.price().scale() > 2) {
                throw new IllegalArgumentException("商品价格无效: " + id);
            }
            items.add(new OrderItem(product.id(), product.name(), product.price(), quantity));
        }
        return List.copyOf(items);
    }

    private BigDecimal totalOf(List<OrderItem> items) {
        return items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }
}
