package com.learn.learnbackend.model;

import java.util.List;

/** 统一分页结果；page 从 0 开始，total 表示筛选后的总记录数。 */
public record PageResult<T>(List<T> items, int page, int size, long total, long totalPages) {
    public PageResult(List<T> items, int page, int size, long total) {
        this(List.copyOf(items), page, size, total, (total + size - 1) / size);
    }

    public static void validate(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page 不能为负数");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size 必须在 1 至 100 之间");
    }
}
