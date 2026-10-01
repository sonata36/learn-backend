package com.learn.learnbackend.util;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** JDBC 连接和事务工具。DAO 使用 try-with-resources 负责语句及结果集的释放。 */
@Component
public class JdbcUtil {
    private final DataSource dataSource;

    public JdbcUtil(DataSource dataSource) { this.dataSource = dataSource; }

    /** 获取由连接池管理的连接；调用方须关闭连接以归还连接池。 */
    public Connection getConnection() throws SQLException { return dataSource.getConnection(); }

    /** 在一个连接上执行工作；成功提交，失败回滚，最终自动释放连接。 */
    public <T> T inTransaction(SqlWork<T> work) {
        try (Connection connection = getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (Exception exception) {
                try { connection.rollback(); }
                catch (SQLException rollbackFailure) { exception.addSuppressed(rollbackFailure); }
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new JdbcOperationException("事务执行失败，已回滚。", exception);
            } finally {
                try { connection.setAutoCommit(originalAutoCommit); }
                catch (SQLException ignored) { /* 连接关闭时由连接池重置状态。 */ }
            }
        } catch (SQLException exception) {
            throw new JdbcOperationException("数据库连接或事务提交失败。", exception);
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> { T run(Connection connection) throws Exception; }

    /** 将底层 SQL 异常包装成带上下文的运行时异常。 */
    public static class JdbcOperationException extends RuntimeException {
        public JdbcOperationException(String message, Throwable cause) { super(message, cause); }
    }
}
