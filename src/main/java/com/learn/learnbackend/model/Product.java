package com.learn.learnbackend.model;

import java.math.BigDecimal;

/** 商品信息；active=false 表示已停用，但保留记录供历史订单引用。 */
public record Product(Long id, String name, BigDecimal price, boolean active) { }
