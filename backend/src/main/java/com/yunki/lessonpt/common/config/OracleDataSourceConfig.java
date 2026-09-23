package com.yunki.lessonpt.common.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Oracle DataSource는 접속 URL이 있을 때만 만든다.
 *
 * Spring Boot 4는 Hikari가 classpath에 있으면 URL이 없어도 풀을 만들려고 하고,
 * 그 과정에서 드라이버를 못 찾으면 애플리케이션이 바로 죽는다.
 * 접속 정보가 없는 로컬 기동이나 컨텍스트 테스트에서는 풀을 만들지 않고
 * health의 oracle 항목만 DOWN으로 남긴다.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
@EnableConfigurationProperties(DataSourceProperties.class)
public class OracleDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}
