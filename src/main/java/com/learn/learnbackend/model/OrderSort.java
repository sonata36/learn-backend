package com.learn.learnbackend.model;

/** 订单列表排序白名单。 */
public enum OrderSort {
    TIME_ASC("ordered_at ASC"), TIME_DESC("ordered_at DESC"), PRICE_ASC("total_price ASC"), PRICE_DESC("total_price DESC");
    private final String sql;
    OrderSort(String sql) { this.sql = sql; }
    public String sql() { return sql; }
}
