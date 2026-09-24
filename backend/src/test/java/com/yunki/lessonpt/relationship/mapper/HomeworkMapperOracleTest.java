package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import com.yunki.lessonpt.location.service.LocationService;
import com.yunki.lessonpt.relationship.service.HomeworkService;
import com.yunki.lessonpt.relationship.service.StudentMonitoringService;
import com.yunki.lessonpt.relationship.service.TeacherStudentLocationService;
import com.yunki.lessonpt.student.service.StudentService;
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.domain.StudentMonitoring;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 임시 모니터링과 과제를 만든 뒤 롤백한다.
 * sequence 숫자는 확인하지 않는다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class HomeworkMapperOracleTest {

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
    private HomeworkMapper homeworkMapper;

    @Autowired
    private StudentMonitoringService studentMonitoringService;

    @Autowired
    private HomeworkService homeworkService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherStudentLocationService teacherStudentLocationService;

    @Autowired
    private LocationService locationService;

    @Test
    void storesSeveralHomeworksAndRestoresWithoutChangingContent() {
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

        Curriculum curriculum = new Curriculum();
        curriculum.setTeacherId(teacher.getTeacherId());
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
        ContentDetail detailA = detail(category.getCategoryId(), "A1", 1);
        ContentDetail detailB = detail(category.getCategoryId(), "A2", 2);
        contentDetailMapper.insertContentDetail(detailA);
        contentDetailMapper.insertContentDetail(detailB);
        StudentCurriculum enrollment = new StudentCurriculum();
        enrollment.setTeacherStudentLocationId(link.getTeacherStudentLocationId());
        enrollment.setCurriculumId(curriculum.getCurriculumId());
        enrollment.setReenrolled(false);
        enrollment.setStatus(RecordStatus.ACTIVE);
        studentCurriculumMapper.insertStudentCurriculum(enrollment);
        StudentMonitoring monitoringA = monitoring(enrollment.getStudentCurriculumId(), detailA.getContentDetailId(), 1);
        StudentMonitoring monitoringB = monitoring(enrollment.getStudentCurriculumId(), detailB.getContentDetailId(), 2);
        studentMonitoringMapper.insertStudentMonitoring(monitoringA);
        studentMonitoringMapper.insertStudentMonitoring(monitoringB);

        LocalDateTime deadline = LocalDateTime.of(2026, 10, 1, 18, 0);
        Homework first = homework(monitoringA.getMonitoringId(), "싱글 스트로크 연습", null, false, null);
        Homework second = homework(monitoringA.getMonitoringId(), "더블 스트로크 연습", deadline, false, null);
        Homework other = homework(monitoringB.getMonitoringId(), "다른 모니터링 과제", null, false, null);
        assertThat(homeworkMapper.insertHomework(first)).isEqualTo(1);
        assertThat(homeworkMapper.insertHomework(second)).isEqualTo(1);
        assertThat(homeworkMapper.insertHomework(other)).isEqualTo(1);

        Homework reloaded = homeworkMapper.selectHomeworkById(first.getHomeworkId());
        assertThat(first.getHomeworkId()).isNotNull().isPositive();
        assertThat(second.getHomeworkId()).isNotEqualTo(first.getHomeworkId());
        assertThat(reloaded.getHomeworkContent()).isEqualTo("싱글 스트로크 연습");
        assertThat(reloaded.getDeadline()).isNull();
        assertThat(reloaded.getCompleted()).isFalse();
        assertThat(reloaded.getFeedback()).isNull();
        assertThat(reloaded.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(homeworkMapper.selectActiveHomeworksByMonitoringId(monitoringA.getMonitoringId()))
                .extracting(Homework::getHomeworkId)
                .containsExactly(first.getHomeworkId(), second.getHomeworkId());

        first.setHomeworkContent("싱글 스트로크 10분");
        first.setDeadline(deadline);
        first.setCompleted(true);
        first.setFeedback("좋음");
        assertThat(homeworkMapper.updateHomework(first)).isEqualTo(1);
        Homework completed = homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(
                first.getHomeworkId(), monitoringA.getMonitoringId());
        assertThat(completed.getCompleted()).isTrue();
        assertThat(completed.getHomeworkContent()).isEqualTo("싱글 스트로크 10분");
        assertThat(completed.getDeadline()).isEqualTo(deadline);
        assertThat(completed.getFeedback()).isEqualTo("좋음");

        first.setDeadline(null);
        first.setFeedback(null);
        assertThat(homeworkMapper.updateHomework(first)).isEqualTo(1);
        Homework cleared = homeworkMapper.selectActiveHomeworkById(first.getHomeworkId());
        assertThat(cleared.getDeadline()).isNull();
        assertThat(cleared.getFeedback()).isNull();
        assertThat(cleared.getCompleted()).isTrue();

        first.setDeadline(deadline);
        first.setFeedback("좋음");
        assertThat(homeworkMapper.updateHomework(first)).isEqualTo(1);
        assertThat(homeworkMapper.softDeleteHomework(first.getHomeworkId(), monitoringA.getMonitoringId())).isEqualTo(1);
        Homework deleted = homeworkMapper.selectHomeworkById(first.getHomeworkId());
        assertThat(deleted.getStatus()).isEqualTo(RecordStatus.INACTIVE);
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(homeworkMapper.selectActiveHomeworkById(first.getHomeworkId())).isNull();

        assertThat(homeworkMapper.restoreHomework(first)).isEqualTo(1);
        Homework restored = homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(
                first.getHomeworkId(), monitoringA.getMonitoringId());
        assertThat(restored.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(restored.getDeletedAt()).isNull();
        assertThat(restored.getHomeworkContent()).isEqualTo("싱글 스트로크 10분");
        assertThat(restored.getDeadline()).isEqualTo(deadline);
        assertThat(restored.getCompleted()).isTrue();
        assertThat(restored.getFeedback()).isEqualTo("좋음");

        assertThat(homeworkMapper.softDeleteActiveHomeworksByMonitoringId(monitoringA.getMonitoringId())).isEqualTo(2);
        assertThat(homeworkMapper.selectActiveHomeworksByMonitoringId(monitoringA.getMonitoringId())).isEmpty();
        assertThat(homeworkMapper.selectActiveHomeworkById(other.getHomeworkId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
    }

    @Test
    void deletingMonitoringSoftDeletesOnlyItsHomeworksAndRestoreLeavesThemInactive() {
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
        Curriculum curriculum = new Curriculum();
        curriculum.setTeacherId(teacher.getTeacherId());
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
        ContentDetail detailA = detail(category.getCategoryId(), "A1", 1);
        ContentDetail detailB = detail(category.getCategoryId(), "A2", 2);
        ContentDetail detailC = detail(category.getCategoryId(), "A3", 3);
        contentDetailMapper.insertContentDetail(detailA);
        contentDetailMapper.insertContentDetail(detailB);
        contentDetailMapper.insertContentDetail(detailC);
        StudentCurriculum enrollment = new StudentCurriculum();
        enrollment.setTeacherStudentLocationId(link.getTeacherStudentLocationId());
        enrollment.setCurriculumId(curriculum.getCurriculumId());
        enrollment.setReenrolled(false);
        enrollment.setStatus(RecordStatus.ACTIVE);
        studentCurriculumMapper.insertStudentCurriculum(enrollment);
        StudentMonitoring monitoringA = monitoring(enrollment.getStudentCurriculumId(), detailA.getContentDetailId(), 1);
        StudentMonitoring monitoringB = monitoring(enrollment.getStudentCurriculumId(), detailB.getContentDetailId(), 2);
        StudentMonitoring monitoringC = monitoring(enrollment.getStudentCurriculumId(), detailC.getContentDetailId(), 3);
        studentMonitoringMapper.insertStudentMonitoring(monitoringA);
        studentMonitoringMapper.insertStudentMonitoring(monitoringB);
        studentMonitoringMapper.insertStudentMonitoring(monitoringC);
        Homework homeworkA1 = homework(monitoringA.getMonitoringId(), "A1", null, false, null);
        Homework homeworkA2 = homework(monitoringA.getMonitoringId(), "A2", null, true, "좋음");
        Homework homeworkB1 = homework(monitoringB.getMonitoringId(), "B1", null, false, null);
        homeworkMapper.insertHomework(homeworkA1);
        homeworkMapper.insertHomework(homeworkA2);
        homeworkMapper.insertHomework(homeworkB1);

        studentMonitoringService.deleteStudentMonitoring(
                teacher.getTeacherId(), enrollment.getStudentCurriculumId(), monitoringA.getMonitoringId());

        assertThat(studentMonitoringMapper.selectStudentMonitoringById(monitoringA.getMonitoringId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(homeworkMapper.selectHomeworkById(homeworkA1.getHomeworkId()).getStatus()).isEqualTo(RecordStatus.INACTIVE);
        assertThat(homeworkMapper.selectHomeworkById(homeworkA2.getHomeworkId()).getStatus()).isEqualTo(RecordStatus.INACTIVE);
        assertThat(studentMonitoringMapper.selectActiveStudentMonitoringById(monitoringB.getMonitoringId()).getDisplayOrder())
                .isEqualTo(1);
        assertThat(studentMonitoringMapper.selectActiveStudentMonitoringById(monitoringC.getMonitoringId()).getDisplayOrder())
                .isEqualTo(2);
        assertThat(homeworkMapper.selectActiveHomeworkById(homeworkB1.getHomeworkId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);

        StudentMonitoring restored = studentMonitoringService.restoreStudentMonitoring(
                teacher.getTeacherId(), enrollment.getStudentCurriculumId(), monitoringA.getMonitoringId());
        assertThat(restored.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(restored.getDisplayOrder()).isEqualTo(3);
        assertThat(homeworkMapper.selectHomeworkById(homeworkA1.getHomeworkId()).getStatus()).isEqualTo(RecordStatus.INACTIVE);
        assertThat(homeworkMapper.selectHomeworkById(homeworkA2.getHomeworkId()).getStatus()).isEqualTo(RecordStatus.INACTIVE);
    }

    @Test
    void rejectsAFourthActiveHomeworkAndARestoreThatWouldExceedThree() {
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
        Curriculum curriculum = new Curriculum();
        curriculum.setTeacherId(teacher.getTeacherId());
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
        ContentDetail detail = detail(category.getCategoryId(), "A1", 1);
        contentDetailMapper.insertContentDetail(detail);
        StudentCurriculum enrollment = new StudentCurriculum();
        enrollment.setTeacherStudentLocationId(link.getTeacherStudentLocationId());
        enrollment.setCurriculumId(curriculum.getCurriculumId());
        enrollment.setReenrolled(false);
        enrollment.setStatus(RecordStatus.ACTIVE);
        studentCurriculumMapper.insertStudentCurriculum(enrollment);
        StudentMonitoring monitoring = monitoring(enrollment.getStudentCurriculumId(), detail.getContentDetailId(), 1);
        studentMonitoringMapper.insertStudentMonitoring(monitoring);

        Long teacherId = teacher.getTeacherId();
        Long monitoringId = monitoring.getMonitoringId();
        Homework first = homeworkService.createHomework(teacherId, monitoringId, "하나", null, null);
        homeworkService.createHomework(teacherId, monitoringId, "둘", null, null);
        homeworkService.createHomework(teacherId, monitoringId, "셋", null, null);
        assertThatThrownBy(() -> homeworkService.createHomework(teacherId, monitoringId, "넷", null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
        assertThat(homeworkMapper.countActiveHomeworksByMonitoringId(monitoringId)).isEqualTo(3);

        homeworkMapper.softDeleteHomework(first.getHomeworkId(), monitoringId);
        homeworkService.createHomework(teacherId, monitoringId, "다시", null, null);
        assertThat(homeworkMapper.countActiveHomeworksByMonitoringId(monitoringId)).isEqualTo(3);
        assertThatThrownBy(() -> homeworkService.restoreHomework(teacherId, monitoringId, first.getHomeworkId()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
        assertThat(homeworkMapper.countActiveHomeworksByMonitoringId(monitoringId)).isEqualTo(3);
        assertThat(homeworkMapper.selectHomeworkById(first.getHomeworkId()).getStatus()).isEqualTo(RecordStatus.INACTIVE);
    }

    @Test
    void releaseAndLocationDeleteKeepLearningHistoryAndDoNotRestoreChildren() {
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
        Location locationA = location(teacher.getTeacherId(), "장소A-" + UUID.randomUUID(), 1);
        Location locationB = location(teacher.getTeacherId(), "장소B-" + UUID.randomUUID(), 2);
        locationMapper.insertLocation(locationA);
        locationMapper.insertLocation(locationB);
        TeacherStudentLocation linkA = link(relation.getTeacherStudentId(), locationA.getLocationId());
        TeacherStudentLocation linkB = link(relation.getTeacherStudentId(), locationB.getLocationId());
        teacherStudentLocationMapper.insertTeacherStudentLocation(linkA);
        teacherStudentLocationMapper.insertTeacherStudentLocation(linkB);
        Curriculum curriculum = curriculum(teacher.getTeacherId(), "기초", 1);
        curriculumMapper.insertCurriculum(curriculum);
        Category category = category(curriculum.getCurriculumId(), "준비", 1);
        categoryMapper.insertCategory(category);
        ContentDetail detailA = detail(category.getCategoryId(), "A", 1);
        ContentDetail detailB = detail(category.getCategoryId(), "B", 2);
        contentDetailMapper.insertContentDetail(detailA);
        contentDetailMapper.insertContentDetail(detailB);
        StudentCurriculum enrollmentA = enrollment(linkA.getTeacherStudentLocationId(), curriculum.getCurriculumId());
        StudentCurriculum enrollmentB = enrollment(linkB.getTeacherStudentLocationId(), curriculum.getCurriculumId());
        studentCurriculumMapper.insertStudentCurriculum(enrollmentA);
        studentCurriculumMapper.insertStudentCurriculum(enrollmentB);
        StudentMonitoring monitoringA = monitoring(enrollmentA.getStudentCurriculumId(), detailA.getContentDetailId(), 1);
        studentMonitoringMapper.insertStudentMonitoring(monitoringA);
        Homework homeworkA = homework(monitoringA.getMonitoringId(), "연습", null, false, null);
        homeworkMapper.insertHomework(homeworkA);

        Long teacherId = teacher.getTeacherId();
        teacherStudentLocationService.releaseLocation(teacherId, student.getStudentId(), locationA.getLocationId());
        assertThat(teacherStudentLocationMapper.selectTeacherStudentLocationById(linkA.getTeacherStudentLocationId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(studentCurriculumMapper.selectStudentCurriculumById(enrollmentA.getStudentCurriculumId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(studentMonitoringMapper.selectStudentMonitoringById(monitoringA.getMonitoringId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(homeworkMapper.selectHomeworkById(homeworkA.getHomeworkId()).getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(teacherStudentLocationMapper.selectTeacherStudentLocationById(linkB.getTeacherStudentLocationId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(studentCurriculumMapper.selectStudentCurriculumById(enrollmentB.getStudentCurriculumId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);

        teacherStudentLocationService.restoreLocation(teacherId, student.getStudentId(), locationA.getLocationId());
        assertThat(teacherStudentLocationMapper.selectTeacherStudentLocationById(linkA.getTeacherStudentLocationId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(studentCurriculumMapper.selectStudentCurriculumById(enrollmentA.getStudentCurriculumId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);

        locationService.deleteLocation(teacherId, locationA.getLocationId());
        assertThat(locationMapper.selectLocationByIdAndTeacherId(locationA.getLocationId(), teacherId).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(teacherStudentLocationMapper.selectTeacherStudentLocationById(linkA.getTeacherStudentLocationId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(locationMapper.selectLocationByIdAndTeacherId(locationB.getLocationId(), teacherId).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(teacherStudentLocationMapper.selectTeacherStudentLocationById(linkB.getTeacherStudentLocationId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(locationMapper.selectLocationByIdAndTeacherId(locationB.getLocationId(), teacherId).getDisplayOrder())
                .isEqualTo(1);

        locationService.restoreLocation(teacherId, locationA.getLocationId());
        assertThat(locationMapper.selectLocationByIdAndTeacherId(locationA.getLocationId(), teacherId).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(teacherStudentLocationMapper.selectTeacherStudentLocationById(linkA.getTeacherStudentLocationId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(studentCurriculumMapper.selectStudentCurriculumById(enrollmentA.getStudentCurriculumId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);

        studentService.releaseStudent(teacherId, student.getStudentId());
        assertThat(teacherStudentMapper.selectTeacherStudentById(relation.getTeacherStudentId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(studentMapper.selectStudentById(student.getStudentId()).getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(teacherStudentLocationMapper.selectTeacherStudentLocationById(linkB.getTeacherStudentLocationId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(studentCurriculumMapper.selectStudentCurriculumById(enrollmentB.getStudentCurriculumId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(studentMonitoringMapper.selectStudentMonitoringById(monitoringA.getMonitoringId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(homeworkMapper.selectHomeworkById(homeworkA.getHomeworkId()).getStatus()).isEqualTo(RecordStatus.ACTIVE);

        studentService.restoreStudent(teacherId, student.getStudentId());
        assertThat(teacherStudentMapper.selectTeacherStudentById(relation.getTeacherStudentId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(teacherStudentLocationMapper.selectTeacherStudentLocationById(linkB.getTeacherStudentLocationId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(studentCurriculumMapper.selectStudentCurriculumById(enrollmentB.getStudentCurriculumId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
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

    private StudentCurriculum enrollment(Long teacherStudentLocationId, Long curriculumId) {
        StudentCurriculum studentCurriculum = new StudentCurriculum();
        studentCurriculum.setTeacherStudentLocationId(teacherStudentLocationId);
        studentCurriculum.setCurriculumId(curriculumId);
        studentCurriculum.setReenrolled(false);
        studentCurriculum.setStatus(RecordStatus.ACTIVE);
        return studentCurriculum;
    }

    private Teacher teacher() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.homework." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        return teacher;
    }

    private ContentDetail detail(Long categoryId, String name, int displayOrder) {
        ContentDetail contentDetail = new ContentDetail();
        contentDetail.setCategoryId(categoryId);
        contentDetail.setName(name);
        contentDetail.setDisplayOrder(displayOrder);
        contentDetail.setStatus(RecordStatus.ACTIVE);
        return contentDetail;
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

    private Homework homework(
            Long monitoringId, String content, LocalDateTime deadline, boolean completed, String feedback) {
        Homework homework = new Homework();
        homework.setMonitoringId(monitoringId);
        homework.setHomeworkContent(content);
        homework.setDeadline(deadline);
        homework.setCompleted(completed);
        homework.setFeedback(feedback);
        homework.setStatus(RecordStatus.ACTIVE);
        return homework;
    }
}
