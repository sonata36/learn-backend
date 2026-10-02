package com.learn.learnbackend;

import com.learn.learnbackend.model.CustomerOrder;
import com.learn.learnbackend.model.OrderSort;
import com.learn.learnbackend.model.Product;
import com.learn.learnbackend.model.ProductSort;
import com.learn.learnbackend.model.PageResult;
import com.learn.learnbackend.service.OrderService;
import com.learn.learnbackend.service.ProductService;
import com.learn.learnbackend.util.JdbcUtil;
import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 使用 H2（MySQL 兼容模式）验证 MyBatis、Druid、分页、HTTP 和事务。 */
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
    @Autowired DataSource dataSource;
    @Autowired WebApplicationContext webContext;

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
        assertNotNull(orders.findById(order.id()).orElseThrow().deletedAt());
        assertEquals(1, orders.findById(order.id()).orElseThrow().items().size());
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
        assertThrows(DataIntegrityViolationException.class, () -> orders.create(Map.of(costly.id(), 10000)));
        try (Connection c = jdbc.getConnection(); Statement s = c.createStatement(); var rs = s.executeQuery("SELECT COUNT(*) FROM orders")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "订单总价写入失败时，订单主表也应回滚");
        }
        try (Connection c = jdbc.getConnection(); Statement s = c.createStatement(); var rs = s.executeQuery("SELECT COUNT(*) FROM order_items")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "事务回滚应同时清除已插入的订单明细");
        }
    }

    @Test
    void pagesAreSortedAndCountedInDatabase() {
        assertInstanceOf(DruidDataSource.class, dataSource);
        Product first = products.create("A", new BigDecimal("30.00"));
        products.create("B", new BigDecimal("10.00"));
        products.create("C", new BigDecimal("20.00"));
        PageResult<Product> page = products.findPage(ProductSort.ID_ASC, false, 0, 2);
        assertEquals(3, page.total());
        assertEquals(2, page.totalPages());
        assertEquals(first.id(), page.items().getFirst().id());
        assertEquals(1, products.findPage(ProductSort.PRICE_ASC, false, 1, 2).items().size());
        assertTrue(products.findPage(ProductSort.ID_ASC, false, 9, 2).items().isEmpty());
        products.delete(first.id());
        assertEquals(2, products.findPage(ProductSort.ID_ASC, false, 0, 10).total());
        assertEquals(3, products.findPage(ProductSort.ID_ASC, true, 0, 10).total());
        Product remaining = products.findPage(ProductSort.PRICE_ASC, false, 0, 1).items().getFirst();
        CustomerOrder older = orders.create(Map.of(remaining.id(), 1));
        CustomerOrder newer = orders.create(Map.of(remaining.id(), 2));
        PageResult<CustomerOrder> orderPage = orders.findPage(OrderSort.PRICE_DESC, 0, 1);
        assertEquals(2, orderPage.total());
        assertEquals(2, orderPage.totalPages());
        assertEquals(newer.id(), orderPage.items().getFirst().id());
        assertEquals(older.id(), orders.findPage(OrderSort.PRICE_ASC, 0, 1).items().getFirst().id());
        assertThrows(IllegalArgumentException.class, () -> products.findPage(ProductSort.ID_ASC, false, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> orders.findPage(OrderSort.TIME_ASC, 0, 101));
    }

    @Test
    void httpRoutesExposePagedCrudAndValidation() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webContext).build();
        mvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content("{\"name\":\"展示架\",\"price\":25.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("展示架"));
        Product product = products.findAll(ProductSort.ID_ASC, false).getFirst();
        mvc.perform(get("/api/products").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(product.id()));
        mvc.perform(get("/api/products").param("size", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
        mvc.perform(get("/api/products").param("sort", "price;DROP TABLE products"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/products").contentType("application/json")
                        .content("{\"name\":\"\",\"price\":-1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("商品名称不能为空"));
        mvc.perform(post("/api/orders").contentType("application/json")
                        .content("{\"items\":{\"" + product.id() + "\":2}}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalPrice").value(51.0))
                .andExpect(jsonPath("$.items[0].productName").value("展示架"));
        mvc.perform(get("/api/orders").param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPages").value(1));
        long orderId = orders.findAll(OrderSort.TIME_ASC).getFirst().id();
        mvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(2));
        mvc.perform(put("/api/products/" + product.id()).contentType("application/json")
                        .content("{\"name\":\"新展示架\",\"price\":30.00}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("新展示架"));
        assertEquals("展示架", orders.findById(orderId).orElseThrow().items().getFirst().productName());
        mvc.perform(put("/api/orders/" + orderId).contentType("application/json")
                        .content("{\"items\":{\"" + product.id() + "\":3}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPrice").value(90.0));
        mvc.perform(delete("/api/products/" + product.id())).andExpect(status().isNoContent());
        mvc.perform(post("/api/orders").contentType("application/json")
                        .content("{\"items\":{\"" + product.id() + "\":1}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/orders/" + orderId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/orders/" + orderId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedAt").isNotEmpty())
                .andExpect(jsonPath("$.items[0].quantity").value(3));
    }
}
