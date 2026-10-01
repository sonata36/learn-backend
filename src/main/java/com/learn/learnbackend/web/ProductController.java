package com.learn.learnbackend.web;

import com.learn.learnbackend.model.PageResult;
import com.learn.learnbackend.model.Product;
import com.learn.learnbackend.model.ProductSort;
import com.learn.learnbackend.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;

/** 商品 HTTP 接口；删除表示停用。 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService products;
    public ProductController(ProductService products) { this.products = products; }

    @GetMapping
    public PageResult<Product> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(defaultValue = "idAsc") String sort,
                                    @RequestParam(defaultValue = "false") boolean includeInactive) {
        return products.findPage(parseSort(sort), includeInactive, page, size);
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody ProductInput input) {
        if (input == null) throw new IllegalArgumentException("请提供商品信息");
        return products.create(input.name(), input.price());
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable long id, @RequestBody ProductInput input) {
        if (input == null) throw new IllegalArgumentException("请提供商品信息");
        products.update(id, input.name(), input.price());
        return products.findById(id).orElseThrow();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { products.delete(id); }

    private ProductSort parseSort(String sort) {
        return switch (sort.toLowerCase(Locale.ROOT)) {
            case "idasc" -> ProductSort.ID_ASC;
            case "priceasc" -> ProductSort.PRICE_ASC;
            case "pricedesc" -> ProductSort.PRICE_DESC;
            case "nameasc" -> ProductSort.NAME_ASC;
            case "namedesc" -> ProductSort.NAME_DESC;
            default -> throw new IllegalArgumentException("sort 只能是 idAsc、priceAsc、priceDesc、nameAsc、nameDesc");
        };
    }

    public record ProductInput(String name, BigDecimal price) { }
}
