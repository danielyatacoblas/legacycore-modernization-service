package com.portfolio.legacycore.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class SourceDataSourceConfiguration {
    @Bean(destroyMethod = "close")
    @Primary
    @FlywayDataSource
    HikariDataSource targetDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.hikari.maximum-pool-size:8}") int maximumPoolSize) {
        var dataSource = new HikariDataSource();
        dataSource.setPoolName("modern-postgres-pool");
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setMaximumPoolSize(maximumPoolSize);
        return dataSource;
    }

    @Bean(destroyMethod = "close")
    HikariDataSource sourceDataSource(
            @Value("${legacy.datasource.url}") String url,
            @Value("${legacy.datasource.username}") String username,
            @Value("${legacy.datasource.password}") String password,
            @Value("${legacy.datasource.maximum-pool-size:4}") int maximumPoolSize) {
        var dataSource = new HikariDataSource();
        dataSource.setPoolName("legacy-oracle-pool");
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setConnectionTimeout(10_000);
        return dataSource;
    }

    @Bean
    JdbcTemplate sourceJdbcTemplate(@Qualifier("sourceDataSource") DataSource sourceDataSource) {
        var jdbc = new JdbcTemplate(sourceDataSource);
        jdbc.setFetchSize(500);
        return jdbc;
    }

    @Bean
    @Primary
    JdbcTemplate targetJdbcTemplate(@Qualifier("targetDataSource") DataSource targetDataSource) {
        return new JdbcTemplate(targetDataSource);
    }
}
