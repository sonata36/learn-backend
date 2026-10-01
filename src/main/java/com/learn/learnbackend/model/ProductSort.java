package com.learn.learnbackend.model;

/** 仅允许按指定商品字段排序，避免将用户输入拼进 SQL。 */
public enum ProductSort {
    ID_ASC("id ASC"), PRICE_ASC("price ASC"), PRICE_DESC("price DESC"), NAME_ASC("name ASC"), NAME_DESC("name DESC");
    private final String sql;
    ProductSort(String sql) { this.sql = sql; }
    public String sql() { return sql; }
}
