package com.learn.learnbackend.mapper;

import com.learn.learnbackend.model.Product;
import com.learn.learnbackend.model.ProductSort;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 商品 SQL 映射；值由 MyBatis 占位符绑定。 */
@Mapper
public interface ProductMapper {
    @Insert("INSERT INTO products(name, price, active) VALUES (#{name}, #{price}, TRUE)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductInsert product);

    @ConstructorArgs({@Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "price", javaType = BigDecimal.class),
            @Arg(column = "active", javaType = boolean.class)})
    @Select("SELECT id, name, price, active FROM products WHERE id = #{id}")
    Product findById(long id);

    @ConstructorArgs({@Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "price", javaType = BigDecimal.class),
            @Arg(column = "active", javaType = boolean.class)})
    @Select("SELECT id, name, price, active FROM products WHERE id = #{id} AND active = TRUE FOR UPDATE")
    Product findActiveForUpdate(long id);

    @ConstructorArgs({@Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "price", javaType = BigDecimal.class),
            @Arg(column = "active", javaType = boolean.class)})
    @SelectProvider(type = ProductSql.class, method = "list")
    List<Product> list(@Param("sort") ProductSort sort, @Param("includeInactive") boolean includeInactive,
                       @Param("size") Integer size, @Param("offset") Long offset);

    @Select("SELECT COUNT(*) FROM products WHERE (#{includeInactive} = TRUE OR active = TRUE)")
    long count(boolean includeInactive);

    @Update("UPDATE products SET name = #{name}, price = #{price} WHERE id = #{id} AND active = TRUE")
    int update(@Param("id") long id, @Param("name") String name, @Param("price") BigDecimal price);

    @Update("UPDATE products SET active = FALSE WHERE id = #{id} AND active = TRUE")
    int deactivate(long id);

    /** 排序列来自枚举白名单；页码仍由占位符绑定。 */
    class ProductSql {
        public static String list(Map<String, Object> params) {
            ProductSort sort = (ProductSort) params.get("sort");
            String sql = "SELECT id, name, price, active FROM products";
            if (!Boolean.TRUE.equals(params.get("includeInactive"))) sql += " WHERE active = TRUE";
            sql += " ORDER BY " + sort.sql() + ", id ASC";
            if (params.get("size") != null) sql += " LIMIT #{size} OFFSET #{offset}";
            return sql;
        }
    }

    /** 接收自增主键。 */
    class ProductInsert {
        private Long id;
        private final String name;
        private final BigDecimal price;
        public ProductInsert(String name, BigDecimal price) { this.name = name; this.price = price; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
    }
}
