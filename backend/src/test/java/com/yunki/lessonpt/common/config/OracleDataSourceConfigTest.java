package com.yunki.lessonpt.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OracleDataSourceConfigTest {

    @Test
    void readsTnsAdminFromJdbcUrl() {
        assertThat(OracleDataSourceConfig.tnsAdminFromUrl(
                "jdbc:oracle:thin:@dbname_high?TNS_ADMIN=/var/lessonpt/wallet"))
                .isEqualTo("/var/lessonpt/wallet");
        assertThat(OracleDataSourceConfig.tnsAdminFromUrl(
                "jdbc:oracle:thin:@dbname_high?TNS_ADMIN=/var/lessonpt/wallet&oracle.net.ssl=true"))
                .isEqualTo("/var/lessonpt/wallet");
        assertThat(OracleDataSourceConfig.tnsAdminFromUrl(
                "jdbc:oracle:thin:@dbname_high?TNS_ADMIN=\"/var/lessonpt/wallet\""))
                .isEqualTo("/var/lessonpt/wallet");
        assertThat(OracleDataSourceConfig.tnsAdminFromUrl("jdbc:oracle:thin:@//host:1521/svc")).isNull();
    }
}
