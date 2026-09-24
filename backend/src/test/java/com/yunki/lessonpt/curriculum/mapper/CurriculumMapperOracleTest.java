package com.yunki.lessonpt.curriculum.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 임시 강사와 커리큘럼을 만든 뒤 롤백한다.
 * sequence 숫자는 확인하지 않는다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class CurriculumMapperOracleTest {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private CurriculumMapper curriculumMapper;

    @Test
    void insertsUpdatesDeletesAndRestoresWithinTeacherScope() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.curriculum." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        teacherMapper.insertTeacher(teacher);
        assertThat(teacher.getTeacherId()).isNotNull().isPositive();

        Curriculum first = curriculum(teacher.getTeacherId(), "입문", 1);
        Curriculum second = curriculum(teacher.getTeacherId(), "심화", 2);
        curriculumMapper.insertCurriculum(first);
        curriculumMapper.insertCurriculum(second);
        assertThat(first.getCurriculumId()).isNotNull().isPositive();
        assertThat(second.getCurriculumId()).isNotEqualTo(first.getCurriculumId());

        Curriculum stored = curriculumMapper.selectCurriculumById(first.getCurriculumId());
        assertThat(stored.getName()).isEqualTo("입문");
        assertThat(stored.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(curriculumMapper.selectActiveCurriculumsByTeacherId(teacher.getTeacherId()))
                .extracting(Curriculum::getDisplayOrder)
                .containsExactly(1, 2);
        assertThat(curriculumMapper.selectMaxDisplayOrderByTeacherId(teacher.getTeacherId())).isEqualTo(2);

        first.setName("입문 개정");
        assertThat(curriculumMapper.updateCurriculum(first)).isEqualTo(1);
        assertThat(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(first.getCurriculumId(), teacher.getTeacherId())
                .getName()).isEqualTo("입문 개정");
        assertThat(curriculumMapper.selectActiveCurriculumById(first.getCurriculumId()).getDisplayOrder()).isEqualTo(1);

        assertThat(curriculumMapper.softDeleteCurriculum(first.getCurriculumId(), teacher.getTeacherId())).isEqualTo(1);
        assertThat(curriculumMapper.shiftActiveDisplayOrdersDown(teacher.getTeacherId(), 1)).isEqualTo(1);
        assertThat(curriculumMapper.selectActiveCurriculumById(first.getCurriculumId())).isNull();
        assertThat(curriculumMapper.selectActiveCurriculumById(second.getCurriculumId()).getDisplayOrder()).isEqualTo(1);
        assertThat(curriculumMapper.selectCurriculumById(first.getCurriculumId()).getStatus()).isEqualTo(RecordStatus.INACTIVE);

        first.setDisplayOrder(2);
        assertThat(curriculumMapper.restoreCurriculum(first)).isEqualTo(1);
        assertThat(curriculumMapper.selectActiveCurriculumsByTeacherId(teacher.getTeacherId()))
                .extracting(Curriculum::getName, Curriculum::getDisplayOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("심화", 1),
                        org.assertj.core.groups.Tuple.tuple("입문 개정", 2));
    }

    private Curriculum curriculum(Long teacherId, String name, int displayOrder) {
        Curriculum curriculum = new Curriculum();
        curriculum.setTeacherId(teacherId);
        curriculum.setName(name);
        curriculum.setDisplayOrder(displayOrder);
        curriculum.setStatus(RecordStatus.ACTIVE);
        return curriculum;
    }
}
