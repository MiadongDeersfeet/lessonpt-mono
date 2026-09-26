package com.yunki.lessonpt.common.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.yunki.lessonpt.common.diagnostic.OracleContext;
import com.yunki.lessonpt.common.diagnostic.OracleDiagnosticMapper;
import com.yunki.lessonpt.common.diagnostic.SchemaV3Tables;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 접속 환경변수가 있을 때만 Oracle을 읽어서 확인한다.
 * 값이 없으면 실패로 처리하지 않고 건너뛴다.
 * 개발 Schema에 붙어 있어도 조회만 하고 데이터는 쓰지 않는다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class OracleSmokeMapperTest {

    @Autowired
    private SmokeMapper smokeMapper;

    @Autowired
    private OracleDiagnosticMapper diagnosticMapper;

    @Value("${lessonpt.oracle.expected-schema}")
    private String expectedSchema;

    @Test
    void selectOneFromDual() {
        assertThat(smokeMapper.selectOne()).isEqualTo(1);
    }

    @Test
    void readsCurrentOracleContext() {
        OracleContext context = diagnosticMapper.selectContext();

        assertThat(context.getDbName()).isNotBlank();
        assertThat(context.getContainerName()).isNotBlank();
        assertThat(context.getCurrentSchema()).isNotBlank();
        assertThat(context.getCurrentSchema()).isEqualToIgnoringCase(expectedSchema);
    }

    @Test
    void schemaV3TablesExist() {
        assertThat(SchemaV3Tables.EXPECTED_COUNT).isEqualTo(16);
        assertThat(diagnosticMapper.countExistingTables(SchemaV3Tables.NAMES))
                .isEqualTo(SchemaV3Tables.EXPECTED_COUNT);
    }
}
