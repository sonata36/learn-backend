package com.learn.learnbackend.web;

import com.learn.learnbackend.model.CustomerOrder;
import com.learn.learnbackend.model.OrderSort;
import com.learn.learnbackend.model.PageResult;
import com.learn.learnbackend.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;

/** 订单 HTTP 接口；总价只由服务器根据商品价格计算。 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;
    public OrderController(OrderService orders) { this.orders = orders; }

    @GetMapping
    public PageResult<CustomerOrder> list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(defaultValue = "timeDesc") String sort) {
        return orders.findPage(parseSort(sort), page, size);
    }

    @GetMapping("/{id}")
    public CustomerOrder get(@PathVariable long id) {
        return orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerOrder create(@RequestBody OrderInput input) {
        return orders.create(items(input));
    }

    @PutMapping("/{id}")
    public CustomerOrder update(@PathVariable long id, @RequestBody OrderInput input) {
        return orders.update(id, items(input));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { orders.delete(id); }

    private Map<Long, Integer> items(OrderInput input) {
        if (input == null) throw new IllegalArgumentException("请提供订单信息");
        return input.items();
    }

    private OrderSort parseSort(String sort) {
        return switch (sort.toLowerCase(Locale.ROOT)) {
            case "timeasc" -> OrderSort.TIME_ASC;
            case "timedesc" -> OrderSort.TIME_DESC;
            case "priceasc" -> OrderSort.PRICE_ASC;
            case "pricedesc" -> OrderSort.PRICE_DESC;
            default -> throw new IllegalArgumentException("sort 只能是 timeAsc、timeDesc、priceAsc、priceDesc");
        };
    }

    public record OrderInput(Map<Long, Integer> items) { }
}
