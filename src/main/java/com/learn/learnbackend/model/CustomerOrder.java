package com.learn.learnbackend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 订单及其明细。 */
public record CustomerOrder(Long id, LocalDateTime orderedAt, BigDecimal totalPrice, List<OrderItem> items) { }
