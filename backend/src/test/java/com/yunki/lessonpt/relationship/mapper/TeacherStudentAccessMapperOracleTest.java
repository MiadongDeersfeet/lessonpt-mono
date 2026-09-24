package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.ProgressStatus;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Category;
import com.yunki.lessonpt.curriculum.domain.ContentDetail;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.location.domain.Location;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.domain.Homework;
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.domain.StudentMonitoring;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentAccess;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.dto.TeacherStudentAccessResponse;
import com.yunki.lessonpt.relationship.service.TeacherStudentAccessService;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.student.service.StudentService;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 임시 접근권한을 만든 뒤 롤백한다.
 * sequence 숫자는 확인하지 않는다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class TeacherStudentAccessMapperOracleTest {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherStudentMapper teacherStudentMapper;

    @Autowired
    private TeacherStudentAccessMapper teacherStudentAccessMapper;

    @Autowired
    private TeacherStudentAccessService teacherStudentAccessService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private LocationMapper locationMapper;

    @Autowired
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    @Autowired
    private CurriculumMapper curriculumMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ContentDetailMapper contentDetailMapper;

    @Autowired
    private StudentCurriculumMapper studentCurriculumMapper;

    @Autowired
    private StudentMonitoringMapper studentMonitoringMapper;

    @Autowired
    private HomeworkMapper homeworkMapper;

    @Test
    void createRevokeAndReleaseKeepHistoryAndTheOtherTeacher() {
        Teacher teacherA = teacher();
        Teacher teacherB = teacher();
        teacherMapper.insertTeacher(teacherA);
        teacherMapper.insertTeacher(teacherB);
        Student student = new Student();
        student.setName("임시학생");
        student.setEmail("student." + UUID.randomUUID() + "@lessonpt.local");
        student.setStatus(RecordStatus.ACTIVE);
        studentMapper.insertStudent(student);
        TeacherStudent relationA = relation(teacherA.getTeacherId(), student.getStudentId());
        TeacherStudent relationB = relation(teacherB.getTeacherId(), student.getStudentId());
        teacherStudentMapper.insertTeacherStudent(relationA);
        teacherStudentMapper.insertTeacherStudent(relationB);
        Homework homework = learningHistory(teacherA.getTeacherId(), relationA.getTeacherStudentId());

        TeacherStudentAccessResponse created = teacherStudentAccessService.createAccess(
                teacherA.getTeacherId(), student.getStudentId());
        TeacherStudentAccess stored = teacherStudentAccessMapper.selectByTeacherStudentId(relationA.getTeacherStudentId());
        assertThat(stored.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(stored.getPublicAccessKey()).isNotBlank();
        assertThat(stored.getTeacherStudentId()).isEqualTo(relationA.getTeacherStudentId());
        assertThat(stored.getRevokedAt()).isNull();
        assertThat(created.publicAccessKey()).isEqualTo(stored.getPublicAccessKey());

        TeacherStudentAccessResponse other = teacherStudentAccessService.createAccess(
                teacherB.getTeacherId(), student.getStudentId());
        assertThat(other.teacherStudentId()).isEqualTo(relationB.getTeacherStudentId());

        teacherStudentAccessService.revokeAccess(teacherA.getTeacherId(), student.getStudentId());
        TeacherStudentAccess revoked = teacherStudentAccessMapper.selectByTeacherStudentId(relationA.getTeacherStudentId());
        assertThat(revoked.getStatus()).isEqualTo(RecordStatus.INACTIVE);
        assertThat(revoked.getRevokedAt()).isNotNull();
        assertThat(teacherStudentAccessMapper.selectActiveByTeacherStudentId(relationA.getTeacherStudentId())).isNull();
        assertThat(teacherStudentAccessMapper.selectByPublicAccessKey(stored.getPublicAccessKey())).isNull();

        TeacherStudentAccessResponse reactivated = teacherStudentAccessService.createAccess(
                teacherA.getTeacherId(), student.getStudentId());
        TeacherStudentAccess opened = teacherStudentAccessMapper.selectByTeacherStudentId(relationA.getTeacherStudentId());
        assertThat(opened.getTeacherStudentAccessId()).isEqualTo(revoked.getTeacherStudentAccessId());
        assertThat(opened.getPublicAccessKey()).isNotEqualTo(stored.getPublicAccessKey());
        assertThat(opened.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(opened.getRevokedAt()).isNull();
        assertThat(teacherStudentAccessMapper.selectByPublicAccessKey(stored.getPublicAccessKey())).isNull();
        assertThat(teacherStudentAccessMapper.selectByPublicAccessKey(reactivated.publicAccessKey()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(teacherStudentAccessMapper.selectByTeacherStudentId(relationB.getTeacherStudentId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);

        studentService.releaseStudent(teacherB.getTeacherId(), student.getStudentId());
        assertThat(teacherStudentMapper.selectTeacherStudentById(relationB.getTeacherStudentId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(teacherStudentAccessMapper.selectByTeacherStudentId(relationB.getTeacherStudentId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(studentMapper.selectStudentById(student.getStudentId()).getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(teacherStudentMapper.selectTeacherStudentById(relationA.getTeacherStudentId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(studentMonitoringMapper.selectStudentMonitoringById(homework.getMonitoringId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(homeworkMapper.selectHomeworkById(homework.getHomeworkId()).getStatus()).isEqualTo(RecordStatus.ACTIVE);
    }

    @Test
    void secondCreateDoesNotAddAnotherActiveRow() {
        Teacher teacher = teacher();
        teacherMapper.insertTeacher(teacher);
        Student student = new Student();
        student.setName("임시학생");
        student.setEmail("student." + UUID.randomUUID() + "@lessonpt.local");
        student.setStatus(RecordStatus.ACTIVE);
        studentMapper.insertStudent(student);
        TeacherStudent relation = relation(teacher.getTeacherId(), student.getStudentId());
        teacherStudentMapper.insertTeacherStudent(relation);

        TeacherStudentAccessResponse created = teacherStudentAccessService.createAccess(
                teacher.getTeacherId(), student.getStudentId());
        assertThatThrownBy(() -> teacherStudentAccessService.createAccess(teacher.getTeacherId(), student.getStudentId()))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
        assertThat(teacherStudentAccessMapper.selectActiveByTeacherStudentId(relation.getTeacherStudentId())
                .getPublicAccessKey()).isEqualTo(created.publicAccessKey());
    }

    @Test
    void publicAccessKeyIsUnique() {
        Teacher teacherA = teacher();
        Teacher teacherB = teacher();
        teacherMapper.insertTeacher(teacherA);
        teacherMapper.insertTeacher(teacherB);
        Student student = new Student();
        student.setName("임시학생");
        student.setEmail("student." + UUID.randomUUID() + "@lessonpt.local");
        student.setStatus(RecordStatus.ACTIVE);
        studentMapper.insertStudent(student);
        TeacherStudent relationA = relation(teacherA.getTeacherId(), student.getStudentId());
        teacherStudentMapper.insertTeacherStudent(relationA);
        Student otherStudent = new Student();
        otherStudent.setName("다른학생");
        otherStudent.setStatus(RecordStatus.ACTIVE);
        studentMapper.insertStudent(otherStudent);
        TeacherStudent relationB = relation(teacherB.getTeacherId(), otherStudent.getStudentId());
        teacherStudentMapper.insertTeacherStudent(relationB);

        String key = UUID.randomUUID().toString();
        teacherStudentAccessMapper.insertTeacherStudentAccess(access(relationA.getTeacherStudentId(), key));
        assertThatThrownBy(() -> teacherStudentAccessMapper.insertTeacherStudentAccess(
                access(relationB.getTeacherStudentId(), key)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private Homework learningHistory(Long teacherId, Long teacherStudentId) {
        Location location = new Location();
        location.setTeacherId(teacherId);
        location.setName("연습실-" + UUID.randomUUID());
        location.setDisplayOrder(1);
        location.setStatus(RecordStatus.ACTIVE);
        locationMapper.insertLocation(location);
        TeacherStudentLocation link = new TeacherStudentLocation();
        link.setTeacherStudentId(teacherStudentId);
        link.setLocationId(location.getLocationId());
        link.setStatus(RecordStatus.ACTIVE);
        teacherStudentLocationMapper.insertTeacherStudentLocation(link);
        Curriculum curriculum = new Curriculum();
        curriculum.setTeacherId(teacherId);
        curriculum.setName("기초");
        curriculum.setDisplayOrder(1);
        curriculum.setStatus(RecordStatus.ACTIVE);
        curriculumMapper.insertCurriculum(curriculum);
        Category category = new Category();
        category.setCurriculumId(curriculum.getCurriculumId());
        category.setName("준비");
        category.setDisplayOrder(1);
        category.setStatus(RecordStatus.ACTIVE);
        categoryMapper.insertCategory(category);
        ContentDetail detail = new ContentDetail();
        detail.setCategoryId(category.getCategoryId());
        detail.setName("기본");
        detail.setDisplayOrder(1);
        detail.setStatus(RecordStatus.ACTIVE);
        contentDetailMapper.insertContentDetail(detail);
        StudentCurriculum enrollment = new StudentCurriculum();
        enrollment.setTeacherStudentLocationId(link.getTeacherStudentLocationId());
        enrollment.setCurriculumId(curriculum.getCurriculumId());
        enrollment.setReenrolled(false);
        enrollment.setStatus(RecordStatus.ACTIVE);
        studentCurriculumMapper.insertStudentCurriculum(enrollment);
        StudentMonitoring monitoring = new StudentMonitoring();
        monitoring.setStudentCurriculumId(enrollment.getStudentCurriculumId());
        monitoring.setContentDetailId(detail.getContentDetailId());
        monitoring.setDisplayOrder(1);
        monitoring.setProgressStatus(ProgressStatus.YET);
        monitoring.setStatus(RecordStatus.ACTIVE);
        studentMonitoringMapper.insertStudentMonitoring(monitoring);
        Homework homework = new Homework();
        homework.setMonitoringId(monitoring.getMonitoringId());
        homework.setHomeworkContent("연습");
        homework.setCompleted(false);
        homework.setStatus(RecordStatus.ACTIVE);
        homeworkMapper.insertHomework(homework);
        return homework;
    }

    private Teacher teacher() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.access." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        return teacher;
    }

    private TeacherStudent relation(Long teacherId, Long studentId) {
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherId(teacherId);
        relation.setStudentId(studentId);
        relation.setStatus(RecordStatus.ACTIVE);
        return relation;
    }

    private TeacherStudentAccess access(Long teacherStudentId, String publicAccessKey) {
        TeacherStudentAccess access = new TeacherStudentAccess();
        access.setTeacherStudentId(teacherStudentId);
        access.setPublicAccessKey(publicAccessKey);
        access.setStatus(RecordStatus.ACTIVE);
        return access;
    }
}
