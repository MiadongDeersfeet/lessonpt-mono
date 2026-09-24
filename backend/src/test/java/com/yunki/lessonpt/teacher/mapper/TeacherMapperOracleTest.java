package com.yunki.lessonpt.teacher.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.teacher.domain.Teacher;

/**
 * 개발 Schema의 기존 강사만 읽는다.
 * 번호가 몇 번인지는 보지 않고, 상태 매핑과 이메일 조회만 확인한다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class TeacherMapperOracleTest {

    private static final String DEV_TEACHER_EMAIL = "dev.teacher@lessonpt.local";

    @Autowired
    private TeacherMapper teacherMapper;

    @Test
    void readsDevTeacherByEmailAndMapsStatus() {
        Teacher byEmail = teacherMapper.selectActiveTeacherByEmail(DEV_TEACHER_EMAIL);

        assertThat(byEmail).isNotNull();
        assertThat(byEmail.getTeacherId()).isNotNull();
        assertThat(byEmail.getEmail()).isEqualTo(DEV_TEACHER_EMAIL);
        assertThat(byEmail.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(byEmail.getPasswordHash()).isNotBlank();

        Teacher byId = teacherMapper.selectTeacherById(byEmail.getTeacherId());
        assertThat(byId.getEmail()).isEqualTo(DEV_TEACHER_EMAIL);
        assertThat(byId.getStatus()).isEqualTo(RecordStatus.ACTIVE);
    }
}
