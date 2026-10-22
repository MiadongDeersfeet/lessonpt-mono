package com.yunki.lessonpt.common.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.sql.DataSource;

import com.yunki.lessonpt.storage.WalletLocationRewriter;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(OracleDataSourceConfig.class);

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    DataSource dataSource(DataSourceProperties properties) {
        rewriteWalletLocation(properties.getUrl());
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    private void rewriteWalletLocation(String jdbcUrl) {
        String walletDir = System.getenv("TNS_ADMIN");
        if (walletDir == null || walletDir.isBlank()) {
            walletDir = tnsAdminFromUrl(jdbcUrl);
        }
        if (walletDir == null || walletDir.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(walletDir);
            WalletLocationRewriter.rewriteIfNeeded(path);
            log.info(
                    "wallet path={} tnsnames={} sqlnet={} cwallet={}",
                    path.toAbsolutePath().normalize().toString().replace('\\', '/'),
                    Files.isRegularFile(path.resolve("tnsnames.ora")),
                    Files.isRegularFile(path.resolve("sqlnet.ora")),
                    Files.isRegularFile(path.resolve("cwallet.sso")));
            if (!WalletLocationRewriter.jdbcTnsAdminMatches(jdbcUrl, walletDir)) {
                log.warn("wallet jdbc TNS_ADMIN differs from runtime wallet path");
            }
        } catch (IOException exception) {
            log.warn("wallet location rewrite skipped");
        }
    }

    static String tnsAdminFromUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        String marker = "tns_admin=";
        int index = jdbcUrl.toLowerCase(Locale.ROOT).indexOf(marker);
        if (index < 0) {
            return null;
        }
        String rest = jdbcUrl.substring(index + marker.length());
        int end = rest.indexOf('&');
        String value = end < 0 ? rest : rest.substring(0, end);
        return value.isBlank() ? null : value;
    }
}
