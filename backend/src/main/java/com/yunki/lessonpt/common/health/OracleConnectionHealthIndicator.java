package com.yunki.lessonpt.common.health;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Oracle 연결 상태를 Actuator health에 올린다.
 *
 * 접속 정보가 없거나 SELECT 1 FROM DUAL이 실패해도 애플리케이션을 종료하지 않는다.
 * 수업 진도나 평가처럼 도메인 상태를 대신 만들어 주지 않고, health 결과만 바꾼다.
 */
@Component("oracle")
public class OracleConnectionHealthIndicator implements HealthIndicator {

    private final ObjectProvider<SmokeMapper> smokeMapper;

    public OracleConnectionHealthIndicator(ObjectProvider<SmokeMapper> smokeMapper) {
        this.smokeMapper = smokeMapper;
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
            if (Integer.valueOf(1).equals(result)) {
                return Health.up().build();
            }
            return Health.down()
                    .withDetail("result", String.valueOf(result))
                    .build();
        }
        catch (RuntimeException ex) {
            return Health.down(ex).build();
        }
    }
}
