package com.learn.learnbackend.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/** 用 Druid 管理 MyBatis 和旧 JDBC DAO 共用的连接。 */
@Configuration
public class DruidConfig {
    @Bean(destroyMethod = "close")
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driver,
            @Value("${app.datasource.initial-size:1}") int initialSize,
            @Value("${app.datasource.min-idle:1}") int minIdle,
            @Value("${app.datasource.max-active:10}") int maxActive,
            @Value("${app.datasource.max-wait:30000}") long maxWait) {
        DruidDataSource pool = new DruidDataSource();
        pool.setUrl(url);
        pool.setUsername(username);
        pool.setPassword(password);
        pool.setDriverClassName(driver);
        pool.setInitialSize(initialSize);
        pool.setMinIdle(minIdle);
        pool.setMaxActive(maxActive);
        pool.setMaxWait(maxWait);
        pool.setValidationQuery("SELECT 1");
        pool.setTestWhileIdle(true);
        return pool;
    }
}
