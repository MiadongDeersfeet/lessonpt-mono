package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.domain.StudentMonitoring;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 임시 수강과 모니터링을 만든 뒤 롤백한다.
 * sequence 숫자는 확인하지 않는다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class StudentMonitoringMapperOracleTest {

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
    private CategoryMapper categoryMapper;

    @Autowired
    private ContentDetailMapper contentDetailMapper;

    @Autowired
    private StudentCurriculumMapper studentCurriculumMapper;

    @Autowired
    private StudentMonitoringMapper studentMonitoringMapper;

    @Test
    void assignsUpdatesDeletesAndRestoresInsideOneEnrollment() {
        Teacher teacher = teacher();
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
        Location location = new Location();
        location.setTeacherId(teacher.getTeacherId());
        location.setName("연습실");
        location.setDisplayOrder(1);
        location.setStatus(RecordStatus.ACTIVE);
        locationMapper.insertLocation(location);
        TeacherStudentLocation link = new TeacherStudentLocation();
        link.setTeacherStudentId(relation.getTeacherStudentId());
        link.setLocationId(location.getLocationId());
        link.setStatus(RecordStatus.ACTIVE);
        teacherStudentLocationMapper.insertTeacherStudentLocation(link);

        Curriculum curriculum = curriculum(teacher.getTeacherId(), "기초", 1);
        Curriculum otherCurriculum = curriculum(teacher.getTeacherId(), "다른과정", 2);
        curriculumMapper.insertCurriculum(curriculum);
        curriculumMapper.insertCurriculum(otherCurriculum);
        Category categoryA = category(curriculum.getCurriculumId(), "준비", 1);
        Category categoryB = category(curriculum.getCurriculumId(), "연주", 2);
        categoryMapper.insertCategory(categoryA);
        categoryMapper.insertCategory(categoryB);
        ContentDetail detailA1 = detail(categoryA.getCategoryId(), "A1", 1);
        ContentDetail detailA2 = detail(categoryA.getCategoryId(), "A2", 2);
        ContentDetail detailB1 = detail(categoryB.getCategoryId(), "B1", 1);
        contentDetailMapper.insertContentDetail(detailA1);
        contentDetailMapper.insertContentDetail(detailA2);
        contentDetailMapper.insertContentDetail(detailB1);

        StudentCurriculum enrollment = enrollment(link.getTeacherStudentLocationId(), curriculum.getCurriculumId());
        StudentCurriculum otherEnrollment = enrollment(link.getTeacherStudentLocationId(), otherCurriculum.getCurriculumId());
        studentCurriculumMapper.insertStudentCurriculum(enrollment);
        studentCurriculumMapper.insertStudentCurriculum(otherEnrollment);

        StudentMonitoring first = monitoring(enrollment.getStudentCurriculumId(), detailA1.getContentDetailId(), 1);
        studentMonitoringMapper.insertStudentMonitoring(first);
        StudentMonitoring reloaded = studentMonitoringMapper.selectStudentMonitoringById(first.getMonitoringId());
        assertThat(first.getMonitoringId()).isNotNull().isPositive();
        assertThat(reloaded.getDisplayOrder()).isEqualTo(1);
        assertThat(reloaded.getCurrentBpm()).isNull();
        assertThat(reloaded.getProgressStatus()).isEqualTo(ProgressStatus.YET);
        assertThat(reloaded.getStatus()).isEqualTo(RecordStatus.ACTIVE);

        StudentMonitoring second = monitoring(enrollment.getStudentCurriculumId(), detailA2.getContentDetailId(), 2);
        StudentMonitoring third = monitoring(enrollment.getStudentCurriculumId(), detailB1.getContentDetailId(), 3);
        StudentMonitoring other = monitoring(otherEnrollment.getStudentCurriculumId(), detailA1.getContentDetailId(), 1);
        studentMonitoringMapper.insertStudentMonitoring(second);
        studentMonitoringMapper.insertStudentMonitoring(third);
        studentMonitoringMapper.insertStudentMonitoring(other);
        assertThat(studentMonitoringMapper.selectActiveStudentMonitoringsByStudentCurriculumId(
                enrollment.getStudentCurriculumId()))
                .extracting(StudentMonitoring::getDisplayOrder)
                .containsExactly(1, 2, 3);
        assertThat(studentMonitoringMapper.selectMaxDisplayOrderByStudentCurriculumId(enrollment.getStudentCurriculumId()))
                .isEqualTo(3);

        third.setCurrentBpm(120);
        third.setProgressStatus(ProgressStatus.IN_PROGRESS);
        third.setMemo("속도보다 정확도");
        assertThat(studentMonitoringMapper.updateStudentMonitoring(third)).isEqualTo(1);
        third.setProgressStatus(ProgressStatus.COMPLETED);
        assertThat(studentMonitoringMapper.updateStudentMonitoring(third)).isEqualTo(1);
        third.setProgressStatus(ProgressStatus.STOPPED);
        assertThat(studentMonitoringMapper.updateStudentMonitoring(third)).isEqualTo(1);
        StudentMonitoring stopped = studentMonitoringMapper.selectActiveStudentMonitoringById(third.getMonitoringId());
        assertThat(stopped.getCurrentBpm()).isEqualTo(120);
        assertThat(stopped.getProgressStatus()).isEqualTo(ProgressStatus.STOPPED);
        assertThat(stopped.getMemo()).isEqualTo("속도보다 정확도");
        assertThat(stopped.getDisplayOrder()).isEqualTo(3);

        third.setCurrentBpm(130);
        third.setProgressStatus(ProgressStatus.COMPLETED);
        third.setMemo("완료");
        assertThat(studentMonitoringMapper.updateStudentMonitoring(third)).isEqualTo(1);
        third.setCurrentBpm(null);
        third.setMemo(null);
        assertThat(studentMonitoringMapper.updateStudentMonitoring(third)).isEqualTo(1);
        StudentMonitoring cleared = studentMonitoringMapper.selectActiveByStudentCurriculumIdAndContentDetailId(
                enrollment.getStudentCurriculumId(), detailB1.getContentDetailId());
        assertThat(cleared.getCurrentBpm()).isNull();
        assertThat(cleared.getMemo()).isNull();
        assertThat(cleared.getProgressStatus()).isEqualTo(ProgressStatus.COMPLETED);

        third.setCurrentBpm(130);
        third.setMemo("완료");
        assertThat(studentMonitoringMapper.updateStudentMonitoring(third)).isEqualTo(1);
        assertThat(studentMonitoringMapper.softDeleteStudentMonitoring(
                second.getMonitoringId(), enrollment.getStudentCurriculumId())).isEqualTo(1);
        assertThat(studentMonitoringMapper.shiftActiveDisplayOrdersDown(enrollment.getStudentCurriculumId(), 2))
                .isEqualTo(1);
        assertThat(studentMonitoringMapper.selectActiveStudentMonitoringById(second.getMonitoringId())).isNull();
        assertThat(studentMonitoringMapper.selectActiveStudentMonitoringsByStudentCurriculumId(
                enrollment.getStudentCurriculumId()))
                .extracting(StudentMonitoring::getContentDetailId, StudentMonitoring::getDisplayOrder)
                .containsExactly(
                        tuple(detailA1.getContentDetailId(), 1),
                        tuple(detailB1.getContentDetailId(), 2));
        assertThat(studentMonitoringMapper.selectActiveStudentMonitoringById(other.getMonitoringId()).getDisplayOrder())
                .isEqualTo(1);

        assertThat(studentMonitoringMapper.softDeleteStudentMonitoring(
                third.getMonitoringId(), enrollment.getStudentCurriculumId())).isEqualTo(1);
        third.setDisplayOrder(2);
        assertThat(studentMonitoringMapper.restoreStudentMonitoring(third)).isEqualTo(1);
        StudentMonitoring restored = studentMonitoringMapper.selectActiveStudentMonitoringById(third.getMonitoringId());
        assertThat(restored.getDisplayOrder()).isEqualTo(2);
        assertThat(restored.getCurrentBpm()).isEqualTo(130);
        assertThat(restored.getProgressStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(restored.getMemo()).isEqualTo("완료");
        assertThat(studentMonitoringMapper.selectActiveStudentMonitoringById(other.getMonitoringId()).getDisplayOrder())
                .isEqualTo(1);
    }

    private Teacher teacher() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.monitor." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        return teacher;
    }

    private Curriculum curriculum(Long teacherId, String name, int displayOrder) {
        Curriculum curriculum = new Curriculum();
        curriculum.setTeacherId(teacherId);
        curriculum.setName(name);
        curriculum.setDisplayOrder(displayOrder);
        curriculum.setStatus(RecordStatus.ACTIVE);
        return curriculum;
    }

    private Category category(Long curriculumId, String name, int displayOrder) {
        Category category = new Category();
        category.setCurriculumId(curriculumId);
        category.setName(name);
        category.setDisplayOrder(displayOrder);
        category.setStatus(RecordStatus.ACTIVE);
        return category;
    }

    private ContentDetail detail(Long categoryId, String name, int displayOrder) {
        ContentDetail contentDetail = new ContentDetail();
        contentDetail.setCategoryId(categoryId);
        contentDetail.setName(name);
        contentDetail.setDisplayOrder(displayOrder);
        contentDetail.setStatus(RecordStatus.ACTIVE);
        return contentDetail;
    }

    private StudentCurriculum enrollment(Long teacherStudentLocationId, Long curriculumId) {
        StudentCurriculum studentCurriculum = new StudentCurriculum();
        studentCurriculum.setTeacherStudentLocationId(teacherStudentLocationId);
        studentCurriculum.setCurriculumId(curriculumId);
        studentCurriculum.setReenrolled(false);
        studentCurriculum.setStatus(RecordStatus.ACTIVE);
        return studentCurriculum;
    }

    private StudentMonitoring monitoring(Long studentCurriculumId, Long contentDetailId, int displayOrder) {
        StudentMonitoring studentMonitoring = new StudentMonitoring();
        studentMonitoring.setStudentCurriculumId(studentCurriculumId);
        studentMonitoring.setContentDetailId(contentDetailId);
        studentMonitoring.setDisplayOrder(displayOrder);
        studentMonitoring.setProgressStatus(ProgressStatus.YET);
        studentMonitoring.setStatus(RecordStatus.ACTIVE);
        return studentMonitoring;
    }
}
