package com.learn.learnbackend.service;

import com.learn.learnbackend.dao.ProductDao;
import com.learn.learnbackend.model.Product;
import com.learn.learnbackend.model.ProductSort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** 商品业务规则：校验输入，并将删除操作实现为停用。 */
@Service
public class ProductService {
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");
    private final ProductDao products;
    public ProductService(ProductDao products) { this.products = products; }

    public Product create(String name, BigDecimal price) {
        return products.create(validName(name), validPrice(price));
    }
    public Optional<Product> findById(long id) { return products.findById(id); }
    public List<Product> findAll(ProductSort sort, boolean includeInactive) { return products.findAll(sort, includeInactive); }
    public void update(long id, String name, BigDecimal price) {
        if (!products.update(id, validName(name), validPrice(price))) throw new IllegalArgumentException("商品不存在或已停用: " + id);
    }
    public void delete(long id) {
        if (!products.deactivate(id)) throw new IllegalArgumentException("商品不存在或已经停用: " + id);
    }

    private String validName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("商品名称不能为空");
        String result = name.trim();
        if (result.length() > 120) throw new IllegalArgumentException("商品名称不能超过 120 个字符");
        return result;
    }
    private BigDecimal validPrice(BigDecimal price) {
        if (price == null || price.signum() < 0 || price.scale() > 2 || price.compareTo(MAX_PRICE) > 0) throw new IllegalArgumentException("价格须为 0 至 9999999999.99 的有效金额（最多两位小数）");
        return price.setScale(2);
    }
}
