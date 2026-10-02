package com.learn.learnbackend.mapper;

import com.learn.learnbackend.model.OrderItem;
import com.learn.learnbackend.model.OrderSort;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 订单头与明细 SQL 映射。 */
@Mapper
public interface OrderMapper {
    @Insert("INSERT INTO orders(total_price) VALUES (0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderInsert order);

    @Insert("INSERT INTO order_items(order_id, product_id, product_name_snapshot, unit_price, quantity) "
            + "VALUES (#{orderId}, #{item.productId}, #{item.productName}, #{item.unitPrice}, #{item.quantity})")
    int insertItem(@Param("orderId") long orderId, @Param("item") OrderItem item);

    @Update("UPDATE orders SET total_price = #{total} WHERE id = #{id} AND deleted_at IS NULL")
    int updateTotal(@Param("id") long id, @Param("total") BigDecimal total);

    @ConstructorArgs({@Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "ordered_at", javaType = LocalDateTime.class),
            @Arg(column = "total_price", javaType = BigDecimal.class),
            @Arg(column = "deleted_at", javaType = LocalDateTime.class)})
    @Select("SELECT id, ordered_at, total_price, deleted_at FROM orders WHERE id = #{id}")
    OrderHeader findById(long id);

    @ConstructorArgs({@Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "ordered_at", javaType = LocalDateTime.class),
            @Arg(column = "total_price", javaType = BigDecimal.class),
            @Arg(column = "deleted_at", javaType = LocalDateTime.class)})
    @Select("SELECT id, ordered_at, total_price, deleted_at FROM orders "
            + "WHERE id = #{id} AND deleted_at IS NULL FOR UPDATE")
    OrderHeader findActiveForUpdate(long id);

    @ConstructorArgs({@Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "ordered_at", javaType = LocalDateTime.class),
            @Arg(column = "total_price", javaType = BigDecimal.class),
            @Arg(column = "deleted_at", javaType = LocalDateTime.class)})
    @SelectProvider(type = OrderSql.class, method = "list")
    List<OrderHeader> list(@Param("sort") OrderSort sort, @Param("size") Integer size,
                           @Param("offset") Long offset);

    @Select("SELECT COUNT(*) FROM orders")
    long count();

    @ConstructorArgs({@Arg(column = "product_id", javaType = Long.class),
            @Arg(column = "product_name_snapshot", javaType = String.class),
            @Arg(column = "unit_price", javaType = BigDecimal.class),
            @Arg(column = "quantity", javaType = int.class)})
    @Select("SELECT product_id, product_name_snapshot, unit_price, quantity FROM order_items "
            + "WHERE order_id = #{orderId} ORDER BY id ASC")
    List<OrderItem> findItems(long orderId);

    @Delete("DELETE FROM order_items WHERE order_id = #{orderId}")
    int deleteItems(long orderId);

    /** 保留订单头和明细，仅记录删除时间。 */
    @Update("UPDATE orders SET deleted_at = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted_at IS NULL")
    int markDeleted(long id);

    record OrderHeader(Long id, LocalDateTime orderedAt, BigDecimal totalPrice, LocalDateTime deletedAt) { }

    class OrderInsert {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    class OrderSql {
        public static String list(Map<String, Object> params) {
            OrderSort sort = (OrderSort) params.get("sort");
            String sql = "SELECT id, ordered_at, total_price, deleted_at FROM orders ORDER BY " + sort.sql() + ", id ASC";
            if (params.get("size") != null) sql += " LIMIT #{size} OFFSET #{offset}";
            return sql;
        }
    }
}
