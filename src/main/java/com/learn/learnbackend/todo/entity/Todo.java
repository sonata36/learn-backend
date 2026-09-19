package com.learn.learnbackend.todo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Todo 实体。
 * 阶段 2b 新增 userId 字段用于归属当前用户（届时通过新迁移脚本 ALTER TABLE 加入）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Todo {

    private Long id;
    private String title;
    private String content;
    private Boolean done;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
