package com.learn.learnbackend.service;

import com.learn.learnbackend.mapper.OrderMapper;
import com.learn.learnbackend.mapper.ProductMapper;
import com.learn.learnbackend.model.CustomerOrder;
import com.learn.learnbackend.model.OrderItem;
import com.learn.learnbackend.model.OrderSort;
import com.learn.learnbackend.model.PageResult;
import com.learn.learnbackend.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 订单业务规则；MyBatis 使用同一个 Spring 事务管理器。 */
@Service
public class OrderService {
    private final ProductMapper products;
    private final OrderMapper orders;

    public OrderService(ProductMapper products, OrderMapper orders) {
        this.products = products;
        this.orders = orders;
    }

    /** 校验、锁定商品并原子写入订单头、明细及总价。 */
    @Transactional
    public CustomerOrder create(Map<Long, Integer> quantities) {
        List<OrderItem> items = validatedItems(quantities);
        OrderMapper.OrderInsert row = new OrderMapper.OrderInsert();
        orders.insert(row);
        long id = row.getId();
        for (OrderItem item : items) orders.insertItem(id, item);
        orders.updateTotal(id, totalOf(items));
        return findById(id).orElseThrow();
    }

    /** 替换订单商品；任何写入失败均回滚到修改前。 */
    @Transactional
    public CustomerOrder update(long orderId, Map<Long, Integer> quantities) {
        if (orders.findActiveForUpdate(orderId) == null)
            throw new IllegalArgumentException("订单不存在或已删除: " + orderId);
        List<OrderItem> items = validatedItems(quantities);
        orders.deleteItems(orderId);
        for (OrderItem item : items) orders.insertItem(orderId, item);
        orders.updateTotal(orderId, totalOf(items));
        return findById(orderId).orElseThrow();
    }

    /** 详情包含完整明细，列表仅返回订单摘要。 */
    @Transactional(readOnly = true)
    public Optional<CustomerOrder> findById(long id) {
        OrderMapper.OrderHeader row = orders.findById(id);
        return row == null ? Optional.empty() : Optional.of(
                new CustomerOrder(row.id(), row.orderedAt(), row.totalPrice(), row.deletedAt(), orders.findItems(id)));
    }

    public List<CustomerOrder> findAll(OrderSort sort) {
        return orders.list(sort, null, null).stream().map(this::summary).toList();
    }

    /** 分页查询订单摘要，页序在数据库执行，避免内存切片。 */
    @Transactional(readOnly = true)
    public PageResult<CustomerOrder> findPage(OrderSort sort, int page, int size) {
        PageResult.validate(page, size);
        return new PageResult<>(orders.list(sort, size, (long) page * size).stream().map(this::summary).toList(),
                page, size, orders.count());
    }

    /** 软删除订单；订单头、下单时间、成交金额与商品快照均保留。 */
    @Transactional
    public void delete(long id) {
        if (orders.markDeleted(id) != 1) throw new IllegalArgumentException("订单不存在或已删除: " + id);
    }

    private CustomerOrder summary(OrderMapper.OrderHeader row) {
        return new CustomerOrder(row.id(), row.orderedAt(), row.totalPrice(), row.deletedAt(), List.of());
    }

    private List<OrderItem> validatedItems(Map<Long, Integer> quantities) {
        if (quantities == null || quantities.isEmpty()) throw new IllegalArgumentException("订单至少需要一个商品");
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long id = entry.getKey();
            Integer quantity = entry.getValue();
            if (id == null || id <= 0) throw new IllegalArgumentException("商品编号必须为正数");
            if (quantity == null || quantity <= 0 || quantity > 10000)
                throw new IllegalArgumentException("商品数量必须在 1 至 10000 之间");
            Product product = products.findActiveForUpdate(id);
            if (product == null) throw new IllegalArgumentException("商品不存在或已停用: " + id);
            if (product.price() == null || product.price().signum() < 0 || product.price().scale() > 2)
                throw new IllegalArgumentException("商品价格无效: " + id);
            items.add(new OrderItem(product.id(), product.name(), product.price(), quantity));
        }
        return List.copyOf(items);
    }

    private BigDecimal totalOf(List<OrderItem> items) {
        return items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }
}
