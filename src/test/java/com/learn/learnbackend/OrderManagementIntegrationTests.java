package com.learn.learnbackend;

import com.learn.learnbackend.model.CustomerOrder;
import com.learn.learnbackend.model.OrderSort;
import com.learn.learnbackend.model.Product;
import com.learn.learnbackend.model.ProductSort;
import com.learn.learnbackend.service.OrderService;
import com.learn.learnbackend.service.ProductService;
import com.learn.learnbackend.util.JdbcUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 使用 H2（MySQL 兼容模式）验证 JDBC CRUD、校验、事务、排序和 SQL 注入防护。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:orders;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "app.console.enabled=false", "spring.sql.init.mode=never"
})
class OrderManagementIntegrationTests {
    @Autowired ProductService products;
    @Autowired OrderService orders;
    @Autowired JdbcUtil jdbc;

    @BeforeEach
    void prepareTables() throws Exception {
        try (Connection c = jdbc.getConnection(); Statement s = c.createStatement()) {
            String ddl = new String(getClass().getResourceAsStream("/db/schema-test.sql").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String line : ddl.split(";")) if (!line.isBlank()) s.execute(line);
            s.executeUpdate("DELETE FROM order_items");
            s.executeUpdate("DELETE FROM orders");
            s.executeUpdate("DELETE FROM products");
        }
    }

    @Test
    void productCrudSortingAndSqlInjectionInputAreSafe() {
        Product expensive = products.create("棚架", new BigDecimal("40.00"));
        Product cheap = products.create("' OR 1=1 --", new BigDecimal("10.00"));
        assertEquals(2, products.findAll(ProductSort.PRICE_ASC, false).size());
        assertEquals(cheap.id(), products.findAll(ProductSort.PRICE_ASC, false).getFirst().id());
        assertEquals(expensive.id(), products.findAll(ProductSort.ID_ASC, false).getFirst().id());
        products.update(expensive.id(), "加固棚架", new BigDecimal("45.50"));
        assertEquals("加固棚架", products.findById(expensive.id()).orElseThrow().name());
        products.delete(cheap.id());
        assertTrue(products.findAll(ProductSort.NAME_ASC, false).stream().noneMatch(p -> p.id().equals(cheap.id())));
        assertFalse(products.findById(cheap.id()).orElseThrow().active());
        assertThrows(IllegalArgumentException.class, () -> products.create("坏价格", new BigDecimal("-1.00")));
    }

    @Test
    void orderCrudSnapshotsPriceAndSupportsSorting() {
        Product table = products.create("折叠桌", new BigDecimal("120.00"));
        Product light = products.create("工作灯", new BigDecimal("35.50"));
        CustomerOrder order = orders.create(Map.of(table.id(), 2, light.id(), 1));
        assertEquals(new BigDecimal("275.50"), order.totalPrice());
        assertEquals(2, order.items().size());
        products.update(table.id(), "新款折叠桌", new BigDecimal("999.00"));
        CustomerOrder historic = orders.findById(order.id()).orElseThrow();
        var historicTable = historic.items().stream().filter(item -> item.productId().equals(table.id())).findFirst().orElseThrow();
        assertEquals("折叠桌", historicTable.productName());
        assertEquals(new BigDecimal("120.00"), historicTable.unitPrice());
        assertEquals(order.id(), orders.findAll(OrderSort.PRICE_DESC).getFirst().id());
        CustomerOrder updated = orders.update(order.id(), Map.of(light.id(), 3));
        assertEquals(new BigDecimal("106.50"), updated.totalPrice());
        assertEquals(1, updated.items().size());
        orders.delete(order.id());
        assertTrue(orders.findById(order.id()).isEmpty());
    }

    @Test
    void invalidOrInactiveProductsAreRejectedWithoutChangingExistingOrder() {
        Product product = products.create("地毯", new BigDecimal("80.00"));
        CustomerOrder order = orders.create(Map.of(product.id(), 1));
        assertThrows(IllegalArgumentException.class, () -> orders.create(Map.of(999999L, 1)));
        assertThrows(IllegalArgumentException.class, () -> orders.create(Map.of(product.id(), 0)));
        assertThrows(IllegalArgumentException.class, () -> orders.create(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> orders.update(order.id(), Map.of(999999L, 1)));
        products.delete(product.id());
        assertThrows(IllegalArgumentException.class, () -> orders.create(Map.of(product.id(), 1)));
        assertEquals(new BigDecimal("80.00"), orders.findById(order.id()).orElseThrow().totalPrice());
    }

    @Test
    void orderCanSortByPlacedTimeInBothDirections() throws Exception {
        Product product = products.create("收纳箱", new BigDecimal("20.00"));
        CustomerOrder first = orders.create(Map.of(product.id(), 1));
        CustomerOrder second = orders.create(Map.of(product.id(), 2));
        try (Connection c = jdbc.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE orders SET ordered_at = ? WHERE id = ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 10, 0))); ps.setLong(2, first.id()); ps.executeUpdate();
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 10, 0))); ps.setLong(2, second.id()); ps.executeUpdate();
        }
        assertEquals(first.id(), orders.findAll(OrderSort.TIME_ASC).getFirst().id());
        assertEquals(second.id(), orders.findAll(OrderSort.TIME_DESC).getFirst().id());
        assertEquals(first.id(), orders.findAll(OrderSort.PRICE_ASC).getFirst().id());
    }

    @Test
    void failedOrderTotalWriteRollsBackHeaderAndItems() throws Exception {
        Product costly = products.create("高价设备", new BigDecimal("9999999999.99"));
        assertThrows(JdbcUtil.JdbcOperationException.class, () -> orders.create(Map.of(costly.id(), 10000)));
        try (Connection c = jdbc.getConnection(); Statement s = c.createStatement(); var rs = s.executeQuery("SELECT COUNT(*) FROM orders")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "订单总价写入失败时，订单主表也应回滚");
        }
        try (Connection c = jdbc.getConnection(); Statement s = c.createStatement(); var rs = s.executeQuery("SELECT COUNT(*) FROM order_items")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "事务回滚应同时清除已插入的订单明细");
        }
    }
}
