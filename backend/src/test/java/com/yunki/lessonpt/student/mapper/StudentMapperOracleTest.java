package com.yunki.lessonpt.student.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.dto.StudentCreateRequest;
import com.yunki.lessonpt.student.dto.StudentResponse;
import com.yunki.lessonpt.student.service.StudentService;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 개발 강사에게 임시 학생을 붙였다가 트랜잭션 롤백으로 지운다.
 * sequence 숫자는 확인하지 않는다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class StudentMapperOracleTest {

    private static final String DEV_TEACHER_EMAIL = "dev.teacher@lessonpt.local";

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherStudentMapper teacherStudentMapper;

    @Autowired
    private StudentService studentService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void insertsStudentAndRelationThenReleasesAndRestores() {
        Teacher teacher = teacherMapper.selectActiveTeacherByEmail(DEV_TEACHER_EMAIL);
        assertThat(teacher).isNotNull();
        String email = "it.student." + UUID.randomUUID() + "@lessonpt.local";

        StudentResponse created = studentService.createStudent(
                teacher.getTeacherId(),
                new StudentCreateRequest("  " + email.toUpperCase() + " ", "임시학생", "010"));

        assertThat(created.studentId()).isNotNull().isPositive();
        assertThat(created.teacherStudentId()).isNotNull().isPositive();
        assertThat(created.email()).isEqualTo(email);

        Student stored = studentMapper.selectStudentById(created.studentId());
        assertThat(stored.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(stored.getEmail()).isEqualTo(email);

        TeacherStudent relation = teacherStudentMapper.selectTeacherStudentById(created.teacherStudentId());
        assertThat(relation.getTeacherId()).isEqualTo(teacher.getTeacherId());
        assertThat(relation.getStudentId()).isEqualTo(created.studentId());
        assertThat(relation.getStatus()).isEqualTo(RecordStatus.ACTIVE);

        studentService.releaseStudent(teacher.getTeacherId(), created.studentId());
        assertThat(studentMapper.selectActiveStudentById(created.studentId())).isNotNull();
        assertThat(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(teacher.getTeacherId(), created.studentId()))
                .isNull();
        assertThat(teacherStudentMapper.selectByTeacherIdAndStudentId(teacher.getTeacherId(), created.studentId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);

        StudentResponse restored = studentService.restoreStudent(teacher.getTeacherId(), created.studentId());
        assertThat(restored.teacherStudentId()).isEqualTo(created.teacherStudentId());
        assertThat(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(teacher.getTeacherId(), created.studentId())
                .getStatus()).isEqualTo(RecordStatus.ACTIVE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollbackLeavesNoFixtureRow() {
        Teacher teacher = teacherMapper.selectActiveTeacherByEmail(DEV_TEACHER_EMAIL);
        assertThat(teacher).isNotNull();
        String email = "it.rollback." + UUID.randomUUID() + "@lessonpt.local";

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        Long studentId = template.execute(status -> {
            StudentResponse created = studentService.createStudent(
                    teacher.getTeacherId(),
                    new StudentCreateRequest(email, "롤백학생", null));
            assertThat(studentMapper.selectStudentById(created.studentId())).isNotNull();
            assertThat(teacherStudentMapper.selectTeacherStudentById(created.teacherStudentId())).isNotNull();
            status.setRollbackOnly();
            return created.studentId();
        });

        assertThat(studentMapper.selectStudentByEmail(email)).isNull();
        assertThat(studentMapper.selectStudentById(studentId)).isNull();
    }
}
