package com.yunki.lessonpt.common.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 테스트 전용 Oracle Schema가 환경변수로 주어졌을 때만 실행한다.
 * 값이 없으면 실패로 처리하지 않고 건너뛴다.
 */
@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class OracleSmokeMapperTest {

    @Autowired
    private SmokeMapper smokeMapper;

    @Test
    void selectOneFromDual() {
        assertThat(smokeMapper.selectOne()).isEqualTo(1);
    }
}
