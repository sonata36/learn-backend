package com.learn.learnbackend.model;

import java.math.BigDecimal;

/** 订单商品快照，价格和名称取下单时的值。 */
public record OrderItem(Long productId, String productName, BigDecimal unitPrice, int quantity) {
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
