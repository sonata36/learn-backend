package com.learn.learnbackend.service;

import com.learn.learnbackend.mapper.ProductMapper;
import com.learn.learnbackend.model.Product;
import com.learn.learnbackend.model.ProductSort;
import com.learn.learnbackend.model.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** 商品业务规则：校验输入，并将删除操作实现为停用。 */
@Service
public class ProductService {
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");
    private final ProductMapper products;
    public ProductService(ProductMapper products) { this.products = products; }

    public Product create(String name, BigDecimal price) {
        ProductMapper.ProductInsert row = new ProductMapper.ProductInsert(validName(name), validPrice(price));
        products.insert(row);
        return products.findById(row.getId());
    }
    public Optional<Product> findById(long id) { return Optional.ofNullable(products.findById(id)); }
    public List<Product> findAll(ProductSort sort, boolean includeInactive) {
        return products.list(sort, includeInactive, null, null);
    }
    /** 0 起始页码；限制页面大小防止一次请求读取过多记录。 */
    @Transactional(readOnly = true)
    public PageResult<Product> findPage(ProductSort sort, boolean includeInactive, int page, int size) {
        PageResult.validate(page, size);
        return new PageResult<>(products.list(sort, includeInactive, size, (long) page * size),
                page, size, products.count(includeInactive));
    }
    public void update(long id, String name, BigDecimal price) {
        if (products.update(id, validName(name), validPrice(price)) != 1) throw new IllegalArgumentException("商品不存在或已停用: " + id);
    }
    public void delete(long id) {
        if (products.deactivate(id) != 1) throw new IllegalArgumentException("商品不存在或已经停用: " + id);
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
