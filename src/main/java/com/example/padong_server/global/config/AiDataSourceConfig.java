package com.example.padong_server.global.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnExpression("'${ai.datasource.jdbc-url:}' != ''")
public class AiDataSourceConfig {

    @Bean(name = "aiDataSource")
    public DataSource aiDataSource(AiDataSourceProperties properties) {
        return DataSourceBuilder.create()
                .url(properties.jdbcUrl())
                .username(properties.username())
                .password(properties.password())
                .driverClassName(properties.driverClassName())
                .build();
    }

    @Bean(name = "aiJdbcTemplate")
    public JdbcTemplate aiJdbcTemplate(@Qualifier("aiDataSource") DataSource aiDataSource) {
        return new JdbcTemplate(aiDataSource);
    }
}
