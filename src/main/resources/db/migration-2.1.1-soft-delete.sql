-- 已有数据库只执行一次。新建数据库直接使用 schema-mysql.sql。
-- 先迁移数据库，再启动包含订单软删除功能的新应用版本。
ALTER TABLE orders ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL;
