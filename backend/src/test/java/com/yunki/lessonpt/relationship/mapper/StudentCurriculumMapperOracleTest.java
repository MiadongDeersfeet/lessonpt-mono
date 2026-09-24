package com.yunki.lessonpt.relationship.mapper;

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
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.location.domain.Location;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 임시 강사, 학생, 장소, 커리큘럼, 수강 배정을 만든 뒤 롤백한다.
 * sequence 숫자는 확인하지 않는다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class StudentCurriculumMapperOracleTest {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherStudentMapper teacherStudentMapper;

    @Autowired
    private LocationMapper locationMapper;

    @Autowired
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    @Autowired
    private CurriculumMapper curriculumMapper;

    @Autowired
    private StudentCurriculumMapper studentCurriculumMapper;

    @Test
    void assignsSameCurriculumAtAnotherLocationAndReenrollsTheInactiveRow() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.enrollment." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        teacherMapper.insertTeacher(teacher);

        Student student = new Student();
        student.setName("임시학생");
        student.setStatus(RecordStatus.ACTIVE);
        studentMapper.insertStudent(student);

        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherId(teacher.getTeacherId());
        relation.setStudentId(student.getStudentId());
        relation.setStatus(RecordStatus.ACTIVE);
        teacherStudentMapper.insertTeacherStudent(relation);

        Location locationA = location(teacher.getTeacherId(), "A", 1);
        Location locationB = location(teacher.getTeacherId(), "B", 2);
        locationMapper.insertLocation(locationA);
        locationMapper.insertLocation(locationB);

        TeacherStudentLocation linkA = link(relation.getTeacherStudentId(), locationA.getLocationId());
        TeacherStudentLocation linkB = link(relation.getTeacherStudentId(), locationB.getLocationId());
        teacherStudentLocationMapper.insertTeacherStudentLocation(linkA);
        teacherStudentLocationMapper.insertTeacherStudentLocation(linkB);

        Curriculum curriculum = new Curriculum();
        curriculum.setTeacherId(teacher.getTeacherId());
        curriculum.setName("기초");
        curriculum.setDisplayOrder(1);
        curriculum.setStatus(RecordStatus.ACTIVE);
        curriculumMapper.insertCurriculum(curriculum);

        String memo = "첫 수강 " + "가".repeat(200);
        StudentCurriculum first = enrollment(linkA.getTeacherStudentLocationId(), curriculum.getCurriculumId(), memo);
        studentCurriculumMapper.insertStudentCurriculum(first);

        StudentCurriculum reloaded = studentCurriculumMapper.selectStudentCurriculumById(first.getStudentCurriculumId());
        assertThat(first.getStudentCurriculumId()).isNotNull().isPositive();
        assertThat(reloaded.getReenrolled()).isFalse();
        assertThat(reloaded.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(reloaded.getDeletedAt()).isNull();
        assertThat(reloaded.getMemo()).isEqualTo(memo);

        StudentCurriculum otherLocation = enrollment(linkB.getTeacherStudentLocationId(), curriculum.getCurriculumId(), null);
        studentCurriculumMapper.insertStudentCurriculum(otherLocation);
        assertThat(studentCurriculumMapper.selectActiveByTeacherStudentLocationIdAndCurriculumId(
                linkB.getTeacherStudentLocationId(), curriculum.getCurriculumId()).getStudentCurriculumId())
                .isEqualTo(otherLocation.getStudentCurriculumId());

        first.setMemo(null);
        assertThat(studentCurriculumMapper.updateStudentCurriculum(first)).isEqualTo(1);
        assertThat(studentCurriculumMapper.selectActiveStudentCurriculumById(first.getStudentCurriculumId()).getMemo())
                .isNull();

        assertThat(studentCurriculumMapper.softDeleteStudentCurriculum(
                first.getStudentCurriculumId(), linkA.getTeacherStudentLocationId())).isEqualTo(1);
        StudentCurriculum deleted = studentCurriculumMapper.selectByTeacherStudentLocationIdAndCurriculumId(
                linkA.getTeacherStudentLocationId(), curriculum.getCurriculumId());
        assertThat(deleted.getStatus()).isEqualTo(RecordStatus.INACTIVE);
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(studentCurriculumMapper.selectActiveByTeacherStudentLocationIdAndCurriculumId(
                linkA.getTeacherStudentLocationId(), curriculum.getCurriculumId())).isNull();

        assertThat(studentCurriculumMapper.restoreStudentCurriculum(first.getStudentCurriculumId())).isEqualTo(1);
        StudentCurriculum reenrolled = studentCurriculumMapper.selectActiveStudentCurriculumById(first.getStudentCurriculumId());
        assertThat(reenrolled.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(reenrolled.getDeletedAt()).isNull();
        assertThat(reenrolled.getReenrolled()).isTrue();
        assertThat(studentCurriculumMapper.selectActiveByTeacherStudentLocationIdAndCurriculumId(
                linkB.getTeacherStudentLocationId(), curriculum.getCurriculumId()).getReenrolled()).isFalse();
    }

    private Location location(Long teacherId, String name, int displayOrder) {
        Location location = new Location();
        location.setTeacherId(teacherId);
        location.setName(name);
        location.setDisplayOrder(displayOrder);
        location.setStatus(RecordStatus.ACTIVE);
        return location;
    }

    private TeacherStudentLocation link(Long teacherStudentId, Long locationId) {
        TeacherStudentLocation link = new TeacherStudentLocation();
        link.setTeacherStudentId(teacherStudentId);
        link.setLocationId(locationId);
        link.setStatus(RecordStatus.ACTIVE);
        return link;
    }

    private StudentCurriculum enrollment(Long teacherStudentLocationId, Long curriculumId, String memo) {
        StudentCurriculum studentCurriculum = new StudentCurriculum();
        studentCurriculum.setTeacherStudentLocationId(teacherStudentLocationId);
        studentCurriculum.setCurriculumId(curriculumId);
        studentCurriculum.setReenrolled(false);
        studentCurriculum.setMemo(memo);
        studentCurriculum.setStatus(RecordStatus.ACTIVE);
        return studentCurriculum;
    }
}
