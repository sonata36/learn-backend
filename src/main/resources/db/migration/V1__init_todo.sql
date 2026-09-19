-- 阶段 1：Todo 表
-- 说明：user_id 归属字段在阶段 2b（Todo 归属用户）时通过新增迁移脚本加入，
--       保持 Flyway 历史与开发阶段一一对应。
CREATE TABLE todo (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL,
    content     TEXT         NULL,
    done        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_todo_title (title)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
