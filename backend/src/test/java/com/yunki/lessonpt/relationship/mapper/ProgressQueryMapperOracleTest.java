package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;
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
import com.yunki.lessonpt.relationship.query.StudentCurriculumProgressView;
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
class ProgressQueryMapperOracleTest {

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

    @Autowired
    private ProgressQueryMapper progressQueryMapper;

    @Test
    void countsCompletedMonitoringAgainstEveryActiveContentDetail() {
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
        Location locationA = location(teacher.getTeacherId(), "A", 1);
        Location locationB = location(teacher.getTeacherId(), "B", 2);
        locationMapper.insertLocation(locationA);
        locationMapper.insertLocation(locationB);
        TeacherStudentLocation linkA = link(relation.getTeacherStudentId(), locationA.getLocationId());
        TeacherStudentLocation linkB = link(relation.getTeacherStudentId(), locationB.getLocationId());
        teacherStudentLocationMapper.insertTeacherStudentLocation(linkA);
        teacherStudentLocationMapper.insertTeacherStudentLocation(linkB);

        Curriculum curriculum = curriculum(teacher.getTeacherId(), "기초", 1);
        Curriculum emptyCurriculum = curriculum(teacher.getTeacherId(), "빈과정", 2);
        curriculumMapper.insertCurriculum(curriculum);
        curriculumMapper.insertCurriculum(emptyCurriculum);
        Category categoryA = category(curriculum.getCurriculumId(), "준비", 1);
        Category categoryB = category(curriculum.getCurriculumId(), "연주", 2);
        categoryMapper.insertCategory(categoryA);
        categoryMapper.insertCategory(categoryB);
        ContentDetail detailA = detail(categoryA.getCategoryId(), "A", 1);
        ContentDetail detailB = detail(categoryA.getCategoryId(), "B", 2);
        ContentDetail detailC = detail(categoryB.getCategoryId(), "C", 1);
        ContentDetail detailD = detail(categoryB.getCategoryId(), "D", 2);
        contentDetailMapper.insertContentDetail(detailA);
        contentDetailMapper.insertContentDetail(detailB);
        contentDetailMapper.insertContentDetail(detailC);
        contentDetailMapper.insertContentDetail(detailD);

        StudentCurriculum first = enrollment(linkA.getTeacherStudentLocationId(), curriculum.getCurriculumId());
        StudentCurriculum second = enrollment(linkB.getTeacherStudentLocationId(), curriculum.getCurriculumId());
        StudentCurriculum empty = enrollment(linkA.getTeacherStudentLocationId(), emptyCurriculum.getCurriculumId());
        studentCurriculumMapper.insertStudentCurriculum(first);
        studentCurriculumMapper.insertStudentCurriculum(second);
        studentCurriculumMapper.insertStudentCurriculum(empty);

        assertProgress(first.getStudentCurriculumId(), 0, 4);
        assertProgress(empty.getStudentCurriculumId(), 0, 0);
        assertThat(progressQueryMapper.selectProgressByStudentCurriculumIds(List.of())).isEmpty();

        StudentMonitoring monitoringA = monitoring(first.getStudentCurriculumId(), detailA.getContentDetailId(), 1, ProgressStatus.COMPLETED);
        StudentMonitoring monitoringB = monitoring(first.getStudentCurriculumId(), detailB.getContentDetailId(), 2, ProgressStatus.IN_PROGRESS);
        StudentMonitoring monitoringC = monitoring(first.getStudentCurriculumId(), detailC.getContentDetailId(), 3, ProgressStatus.STOPPED);
        StudentMonitoring other = monitoring(second.getStudentCurriculumId(), detailA.getContentDetailId(), 1, ProgressStatus.IN_PROGRESS);
        studentMonitoringMapper.insertStudentMonitoring(monitoringA);
        studentMonitoringMapper.insertStudentMonitoring(monitoringB);
        studentMonitoringMapper.insertStudentMonitoring(monitoringC);
        studentMonitoringMapper.insertStudentMonitoring(other);
        assertProgress(first.getStudentCurriculumId(), 1, 4);
        assertProgress(second.getStudentCurriculumId(), 0, 4);

        monitoringB.setProgressStatus(ProgressStatus.COMPLETED);
        studentMonitoringMapper.updateStudentMonitoring(monitoringB);
        assertProgress(first.getStudentCurriculumId(), 2, 4);
        assertProgress(second.getStudentCurriculumId(), 0, 4);

        monitoringA.setProgressStatus(ProgressStatus.YET);
        monitoringB.setProgressStatus(ProgressStatus.COMPLETED);
        studentMonitoringMapper.updateStudentMonitoring(monitoringA);
        studentMonitoringMapper.updateStudentMonitoring(monitoringB);
        studentMonitoringMapper.softDeleteStudentMonitoring(monitoringC.getMonitoringId(), first.getStudentCurriculumId());
        assertProgress(first.getStudentCurriculumId(), 1, 4);

        monitoringA.setProgressStatus(ProgressStatus.COMPLETED);
        studentMonitoringMapper.updateStudentMonitoring(monitoringA);
        studentMonitoringMapper.softDeleteStudentMonitoring(monitoringA.getMonitoringId(), first.getStudentCurriculumId());
        assertProgress(first.getStudentCurriculumId(), 1, 4);

        contentDetailMapper.softDeleteContentDetail(detailB.getContentDetailId(), categoryA.getCategoryId());
        assertProgress(first.getStudentCurriculumId(), 0, 3);

        assertThat(progressQueryMapper.selectProgressByStudentCurriculumIds(List.of(
                first.getStudentCurriculumId(), second.getStudentCurriculumId())))
                .extracting(
                        StudentCurriculumProgressView::getStudentCurriculumId,
                        StudentCurriculumProgressView::getCompletedCount,
                        StudentCurriculumProgressView::getTotalCount)
                .containsExactly(
                        tuple(first.getStudentCurriculumId(), 0, 3),
                        tuple(second.getStudentCurriculumId(), 0, 3));
    }

    @Test
    void returnsOnlyEnrollmentsOwnedByTheTeacherWithActiveParents() {
        Teacher teacher = teacher();
        Teacher otherTeacher = teacher();
        teacherMapper.insertTeacher(teacher);
        teacherMapper.insertTeacher(otherTeacher);
        Student student = new Student();
        student.setName("임시학생");
        student.setStatus(RecordStatus.ACTIVE);
        studentMapper.insertStudent(student);
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherId(teacher.getTeacherId());
        relation.setStudentId(student.getStudentId());
        relation.setStatus(RecordStatus.ACTIVE);
        teacherStudentMapper.insertTeacherStudent(relation);
        Location location = location(teacher.getTeacherId(), "A", 1);
        locationMapper.insertLocation(location);
        TeacherStudentLocation link = link(relation.getTeacherStudentId(), location.getLocationId());
        teacherStudentLocationMapper.insertTeacherStudentLocation(link);
        Curriculum curriculum = curriculum(teacher.getTeacherId(), "기초", 1);
        Curriculum otherCurriculum = curriculum(otherTeacher.getTeacherId(), "다른과정", 1);
        curriculumMapper.insertCurriculum(curriculum);
        curriculumMapper.insertCurriculum(otherCurriculum);
        StudentCurriculum owned = enrollment(link.getTeacherStudentLocationId(), curriculum.getCurriculumId());
        StudentCurriculum foreignCurriculum = enrollment(
                link.getTeacherStudentLocationId(), otherCurriculum.getCurriculumId());
        studentCurriculumMapper.insertStudentCurriculum(owned);
        studentCurriculumMapper.insertStudentCurriculum(foreignCurriculum);

        assertThat(progressQueryMapper.selectOwnedActiveStudentCurriculumIds(
                teacher.getTeacherId(),
                List.of(owned.getStudentCurriculumId(), foreignCurriculum.getStudentCurriculumId())))
                .containsExactly(owned.getStudentCurriculumId());
        assertThat(progressQueryMapper.selectOwnedActiveStudentCurriculumIds(teacher.getTeacherId(), List.of()))
                .isEmpty();

        studentCurriculumMapper.softDeleteStudentCurriculum(
                owned.getStudentCurriculumId(), link.getTeacherStudentLocationId());
        assertThat(progressQueryMapper.selectOwnedActiveStudentCurriculumIds(
                teacher.getTeacherId(), List.of(owned.getStudentCurriculumId()))).isEmpty();
    }

    private void assertProgress(Long studentCurriculumId, int completedCount, int totalCount) {
        StudentCurriculumProgressView progress = progressQueryMapper.selectProgressByStudentCurriculumId(studentCurriculumId);
        assertThat(progress.getStudentCurriculumId()).isEqualTo(studentCurriculumId);
        assertThat(progress.getCompletedCount()).isEqualTo(completedCount);
        assertThat(progress.getTotalCount()).isEqualTo(totalCount);
    }

    private Teacher teacher() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.progress." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        return teacher;
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

    private StudentMonitoring monitoring(
            Long studentCurriculumId, Long contentDetailId, int displayOrder, ProgressStatus progressStatus) {
        StudentMonitoring studentMonitoring = new StudentMonitoring();
        studentMonitoring.setStudentCurriculumId(studentCurriculumId);
        studentMonitoring.setContentDetailId(contentDetailId);
        studentMonitoring.setDisplayOrder(displayOrder);
        studentMonitoring.setProgressStatus(progressStatus);
        studentMonitoring.setStatus(RecordStatus.ACTIVE);
        return studentMonitoring;
    }
}
