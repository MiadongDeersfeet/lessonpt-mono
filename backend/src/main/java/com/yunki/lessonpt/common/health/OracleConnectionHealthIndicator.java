package com.yunki.lessonpt.common.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import com.yunki.lessonpt.common.diagnostic.OracleContext;
import com.yunki.lessonpt.common.diagnostic.OracleDiagnosticMapper;
import com.yunki.lessonpt.common.diagnostic.SchemaV3Tables;

/**
 * Oracle 연결 상태를 Actuator health에 올린다.
 *
 * 접속 정보가 없거나 SELECT 1 FROM DUAL이 실패해도 애플리케이션을 종료하지 않는다.
 * 연결되면 DB, PDB, Schema 이름과 Schema v3 테이블 수만 덧붙인다.
 * 비밀번호, 접속 주소, 호스트는 응답에 넣지 않는다.
 */
@Component("oracle")
public class OracleConnectionHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(OracleConnectionHealthIndicator.class);

    private final ObjectProvider<SmokeMapper> smokeMapper;
    private final ObjectProvider<OracleDiagnosticMapper> diagnosticMapper;

    public OracleConnectionHealthIndicator(
            ObjectProvider<SmokeMapper> smokeMapper,
            ObjectProvider<OracleDiagnosticMapper> diagnosticMapper) {
        this.smokeMapper = smokeMapper;
        this.diagnosticMapper = diagnosticMapper;
    }

    @Override
    public Health health() {
        SmokeMapper mapper = smokeMapper.getIfAvailable();
        if (mapper == null) {
            return Health.down()
                    .withDetail("reason", "Oracle 접속 정보가 없다")
                    .build();
        }

        try {
            Integer result = mapper.selectOne();
            if (!Integer.valueOf(1).equals(result)) {
                return Health.down()
                        .withDetail("smokeQuery", String.valueOf(result))
                        .build();
            }

            Health.Builder up = Health.up().withDetail("smokeQuery", 1);
            addContext(up);
            return up.build();
        }
        catch (RuntimeException ex) {
            // 예외 문장에 호스트나 URL이 섞일 수 있어 응답에는 넣지 않는다.
            log.warn("Oracle health check failed", ex);
            return Health.down()
                    .withDetail("reason", "Oracle 조회에 실패했다")
                    .build();
        }
    }

    private void addContext(Health.Builder up) {
        OracleDiagnosticMapper diagnostic = diagnosticMapper.getIfAvailable();
        if (diagnostic == null) {
            return;
        }
        OracleContext context = diagnostic.selectContext();
        int tableCount = diagnostic.countExistingTables(SchemaV3Tables.NAMES);
        up.withDetail("dbName", context.getDbName())
                .withDetail("containerName", context.getContainerName())
                .withDetail("currentSchema", context.getCurrentSchema())
                .withDetail("schemaTableCount", tableCount);
    }
}
