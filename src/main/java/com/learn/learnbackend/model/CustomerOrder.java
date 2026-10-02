package com.learn.learnbackend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 订单及其历史明细；deletedAt 非空表示已删除，记录仍可查询。 */
public record CustomerOrder(Long id, LocalDateTime orderedAt, BigDecimal totalPrice,
                            LocalDateTime deletedAt, List<OrderItem> items) { }
