package com.yunki.lessonpt.student.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.auth.security.StudentPrincipal;
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
import com.yunki.lessonpt.relationship.mapper.HomeworkMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.relationship.service.TeacherStudentAccessService;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.dto.StudentContentView;
import com.yunki.lessonpt.student.dto.StudentLearningResponse;
import com.yunki.lessonpt.student.mapper.StudentPortalMapper;
import com.yunki.lessonpt.student.query.StudentPortalCategoryRow;
import com.yunki.lessonpt.student.query.StudentPortalMonitoringRow;
import com.yunki.lessonpt.student.service.StudentPortalService;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class StudentPortalMapperOracleTest {

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
    private TeacherStudentAccessMapper teacherStudentAccessMapper;
    @Autowired
    private TeacherStudentAccessService teacherStudentAccessService;
    @Autowired
    private StudentPortalService studentPortalService;
    @Autowired
    private StudentPortalMapper studentPortalMapper;

    @Test
    void showsOnlyTheAuthenticatedStudentsActiveLearningTree() throws Exception {
        Teacher teacher = teacher();
        teacherMapper.insertTeacher(teacher);
        Teacher otherTeacher = teacher();
        teacherMapper.insertTeacher(otherTeacher);
        Student student = student("학생A");
        student.setEmail("portal." + UUID.randomUUID() + "@lessonpt.local");
        studentMapper.insertStudent(student);
        Student otherStudent = student("학생B");
        otherStudent.setEmail("other." + UUID.randomUUID() + "@lessonpt.local");
        studentMapper.insertStudent(otherStudent);
        TeacherStudent relation = relation(teacher.getTeacherId(), student.getStudentId());
        teacherStudentMapper.insertTeacherStudent(relation);
        TeacherStudent otherRelation = relation(otherTeacher.getTeacherId(), otherStudent.getStudentId());
        teacherStudentMapper.insertTeacherStudent(otherRelation);

        Location location = location(teacher.getTeacherId());
        locationMapper.insertLocation(location);
        TeacherStudentLocation link = link(relation.getTeacherStudentId(), location.getLocationId());
        teacherStudentLocationMapper.insertTeacherStudentLocation(link);
        Location otherLocation = location(otherTeacher.getTeacherId());
        locationMapper.insertLocation(otherLocation);
        TeacherStudentLocation otherLink = link(otherRelation.getTeacherStudentId(), otherLocation.getLocationId());
        teacherStudentLocationMapper.insertTeacherStudentLocation(otherLink);

        Curriculum curriculum = curriculum(teacher.getTeacherId(), "기초", 2);
        curriculumMapper.insertCurriculum(curriculum);
        Curriculum later = curriculum(teacher.getTeacherId(), "빈과정", 1);
        curriculumMapper.insertCurriculum(later);
        Curriculum foreign = curriculum(otherTeacher.getTeacherId(), "다른강사", 1);
        curriculumMapper.insertCurriculum(foreign);
        Category second = category(curriculum.getCurriculumId(), "나중", 2);
        categoryMapper.insertCategory(second);
        Category first = category(curriculum.getCurriculumId(), "먼저", 1);
        categoryMapper.insertCategory(first);
        ContentDetail visible = detail(first.getCategoryId(), "싱글", 1);
        visible.setTargetBpm(120);
        visible.setMemo("숨김");
        contentDetailMapper.insertContentDetail(visible);
        ContentDetail unmonitored = detail(first.getCategoryId(), "더블", 2);
        contentDetailMapper.insertContentDetail(unmonitored);
        ContentDetail unpublished = detail(first.getCategoryId(), "비공개루디먼트", 3);
        unpublished.setMemo("학생에게 보이면 안 됨");
        contentDetailMapper.insertContentDetail(unpublished);
        ContentDetail inactive = detail(second.getCategoryId(), "비활성", 1);
        contentDetailMapper.insertContentDetail(inactive);
        contentDetailMapper.softDeleteContentDetail(inactive.getContentDetailId(), inactive.getCategoryId());
        Curriculum emptyParent = later;
        StudentCurriculum enrollment = enrollment(link.getTeacherStudentLocationId(), curriculum.getCurriculumId());
        enrollment.setMemo("배정메모");
        studentCurriculumMapper.insertStudentCurriculum(enrollment);
        studentCurriculumMapper.insertStudentCurriculum(enrollment(link.getTeacherStudentLocationId(), emptyParent.getCurriculumId()));
        StudentCurriculum foreignEnrollment = enrollment(otherLink.getTeacherStudentLocationId(), foreign.getCurriculumId());
        studentCurriculumMapper.insertStudentCurriculum(foreignEnrollment);

        StudentMonitoring learned = monitoring(enrollment.getStudentCurriculumId(), visible.getContentDetailId(), ProgressStatus.COMPLETED);
        learned.setCurrentBpm(90);
        learned.setMemo("모니터링메모");
        studentMonitoringMapper.insertStudentMonitoring(learned);
        StudentMonitoring stopped = monitoring(enrollment.getStudentCurriculumId(), unmonitored.getContentDetailId(), ProgressStatus.STOPPED);
        stopped.setDisplayOrder(2);
        studentMonitoringMapper.insertStudentMonitoring(stopped);
        Homework homework = new Homework();
        homework.setMonitoringId(learned.getMonitoringId());
        homework.setHomeworkContent("연습");
        homework.setCompleted(false);
        homework.setFeedback("좋음");
        homework.setStatus(RecordStatus.ACTIVE);
        homeworkMapper.insertHomework(homework);
        Homework inactiveHomework = new Homework();
        inactiveHomework.setMonitoringId(learned.getMonitoringId());
        inactiveHomework.setHomeworkContent("숨김과제");
        inactiveHomework.setCompleted(true);
        inactiveHomework.setStatus(RecordStatus.ACTIVE);
        homeworkMapper.insertHomework(inactiveHomework);
        homeworkMapper.softDeleteHomework(inactiveHomework.getHomeworkId(), inactiveHomework.getMonitoringId());

        teacherStudentAccessService.createAccess(teacher.getTeacherId(), student.getStudentId());
        TeacherStudentAccess access = teacherStudentAccessMapper.selectActiveByTeacherStudentId(relation.getTeacherStudentId());
        StudentPrincipal principal = new StudentPrincipal(
                access.getTeacherStudentAccessId(), relation.getTeacherStudentId(), student.getStudentId());

        assertThat(studentPortalService.me(principal).name()).isEqualTo("학생A");
        StudentLearningResponse learning = studentPortalService.learning(principal);
        assertThat(learning.curriculums()).extracting(view -> view.name()).containsExactly("빈과정", "기초");
        assertThat(learning.curriculums().get(0).progress()).isNull();
        assertThat(learning.curriculums().get(1).categories()).extracting(view -> view.name()).containsExactly("먼저", "나중");
        assertThat(learning.curriculums().get(1).progress().completedCount()).isEqualTo(1);
        assertThat(learning.curriculums().get(1).progress().totalCount()).isEqualTo(3);
        assertThat(learning.curriculums().get(1).categories().get(0).totalContentCount()).isEqualTo(3);
        assertThat(learning.curriculums().get(1).categories().get(0).contents())
                .extracting(StudentContentView::name)
                .containsExactly("싱글", "더블");
        StudentContentView firstContent = learning.curriculums().get(1).categories().get(0).contents().get(0);
        StudentContentView secondContent = learning.curriculums().get(1).categories().get(0).contents().get(1);
        assertThat(firstContent.monitoringId()).isEqualTo(learned.getMonitoringId());
        assertThat(firstContent.name()).isEqualTo("싱글");
        assertThat(firstContent.currentBpm()).isEqualTo(90);
        assertThat(firstContent.homeworks()).extracting(item -> item.content()).containsExactly("연습");
        assertThat(firstContent.homeworks().get(0).homeworkId()).isEqualTo(homework.getHomeworkId());
        assertThat(secondContent.monitoringId()).isEqualTo(stopped.getMonitoringId());
        assertThat(secondContent.progressStatus()).isEqualTo(ProgressStatus.STOPPED);
        assertThat(learning.curriculums().get(1).categories().get(1).totalContentCount()).isEqualTo(0);
        assertThat(learning.curriculums().get(1).categories().get(1).contents()).isEmpty();
        assertThat(learning.curriculums()).extracting(view -> view.name()).doesNotContain("다른강사");
        assertThat(firstContent.homeworks()).extracting(item -> item.content()).doesNotContain("숨김과제");
    }

    @Test
    void categoryCountsAndMonitoringQueryKeepUnassignedDetailsUnread() throws Exception {
        Teacher teacher = teacher();
        teacherMapper.insertTeacher(teacher);
        Student student = student("학생A");
        student.setEmail("boundary." + UUID.randomUUID() + "@lessonpt.local");
        studentMapper.insertStudent(student);
        Student otherStudent = student("학생B");
        otherStudent.setEmail("boundary.other." + UUID.randomUUID() + "@lessonpt.local");
        studentMapper.insertStudent(otherStudent);
        TeacherStudent relation = relation(teacher.getTeacherId(), student.getStudentId());
        teacherStudentMapper.insertTeacherStudent(relation);
        TeacherStudent otherRelation = relation(teacher.getTeacherId(), otherStudent.getStudentId());
        teacherStudentMapper.insertTeacherStudent(otherRelation);
        Location location = location(teacher.getTeacherId());
        locationMapper.insertLocation(location);
        TeacherStudentLocation link = link(relation.getTeacherStudentId(), location.getLocationId());
        teacherStudentLocationMapper.insertTeacherStudentLocation(link);
        Location otherLocation = location(teacher.getTeacherId());
        otherLocation.setName("다른연습실");
        otherLocation.setDisplayOrder(2);
        locationMapper.insertLocation(otherLocation);
        TeacherStudentLocation otherLink = link(otherRelation.getTeacherStudentId(), otherLocation.getLocationId());
        teacherStudentLocationMapper.insertTeacherStudentLocation(otherLink);

        Curriculum curriculum = curriculum(teacher.getTeacherId(), "기초", 1);
        curriculumMapper.insertCurriculum(curriculum);
        Category categoryA = category(curriculum.getCurriculumId(), "A", 1);
        categoryMapper.insertCategory(categoryA);
        Category categoryB = category(curriculum.getCurriculumId(), "B", 2);
        categoryMapper.insertCategory(categoryB);
        Category categoryC = category(curriculum.getCurriculumId(), "C", 3);
        categoryMapper.insertCategory(categoryC);
        Category hiddenStatus = category(curriculum.getCurriculumId(), "상태", 4);
        categoryMapper.insertCategory(hiddenStatus);

        ContentDetail detailA1 = detail(categoryA.getCategoryId(), "A1", 1);
        contentDetailMapper.insertContentDetail(detailA1);
        ContentDetail detailA2 = detail(categoryA.getCategoryId(), "A2", 2);
        contentDetailMapper.insertContentDetail(detailA2);
        ContentDetail detailA3 = detail(categoryA.getCategoryId(), "A3", 3);
        contentDetailMapper.insertContentDetail(detailA3);
        ContentDetail removed = detail(categoryA.getCategoryId(), "사라진상세", 4);
        contentDetailMapper.insertContentDetail(removed);
        contentDetailMapper.insertContentDetail(detail(categoryB.getCategoryId(), "B1", 1));
        contentDetailMapper.insertContentDetail(detail(categoryB.getCategoryId(), "B2", 2));
        ContentDetail hiddenDetail = detail(hiddenStatus.getCategoryId(), "D1", 1);
        contentDetailMapper.insertContentDetail(hiddenDetail);

        StudentCurriculum enrollment = enrollment(link.getTeacherStudentLocationId(), curriculum.getCurriculumId());
        studentCurriculumMapper.insertStudentCurriculum(enrollment);
        StudentCurriculum otherEnrollment = enrollment(otherLink.getTeacherStudentLocationId(), curriculum.getCurriculumId());
        studentCurriculumMapper.insertStudentCurriculum(otherEnrollment);

        StudentMonitoring learned = monitoring(enrollment.getStudentCurriculumId(), detailA1.getContentDetailId(), ProgressStatus.COMPLETED);
        studentMonitoringMapper.insertStudentMonitoring(learned);
        studentMonitoringMapper.insertStudentMonitoring(
                monitoring(enrollment.getStudentCurriculumId(), detailA2.getContentDetailId(), ProgressStatus.IN_PROGRESS));
        studentMonitoringMapper.insertStudentMonitoring(
                monitoring(otherEnrollment.getStudentCurriculumId(), detailA3.getContentDetailId(), ProgressStatus.YET));
        StudentMonitoring removedMonitoring = monitoring(
                enrollment.getStudentCurriculumId(), removed.getContentDetailId(), ProgressStatus.IN_PROGRESS);
        studentMonitoringMapper.insertStudentMonitoring(removedMonitoring);
        contentDetailMapper.softDeleteContentDetail(removed.getContentDetailId(), categoryA.getCategoryId());
        StudentMonitoring inactiveMonitoring = monitoring(
                enrollment.getStudentCurriculumId(), hiddenDetail.getContentDetailId(), ProgressStatus.YET);
        studentMonitoringMapper.insertStudentMonitoring(inactiveMonitoring);
        studentMonitoringMapper.softDeleteStudentMonitoring(
                inactiveMonitoring.getMonitoringId(), enrollment.getStudentCurriculumId());

        assertThat(studentPortalMapper.selectActiveCategories(java.util.List.of(curriculum.getCurriculumId())))
                .extracting(StudentPortalCategoryRow::getCategoryName, StudentPortalCategoryRow::getTotalContentCount)
                .containsExactly(tuple("A", 3), tuple("B", 2), tuple("C", 0), tuple("상태", 1));
        assertThat(studentPortalMapper.selectActiveMonitoringContents(java.util.List.of(enrollment.getStudentCurriculumId())))
                .extracting(StudentPortalMonitoringRow::getContentName)
                .containsExactly("A1", "A2");
        assertThat(studentPortalMapper.selectActiveMonitoringContents(java.util.List.of(otherEnrollment.getStudentCurriculumId())))
                .extracting(StudentPortalMonitoringRow::getContentName)
                .containsExactly("A3");

        teacherStudentAccessService.createAccess(teacher.getTeacherId(), student.getStudentId());
        TeacherStudentAccess access = teacherStudentAccessMapper.selectActiveByTeacherStudentId(relation.getTeacherStudentId());
        StudentPrincipal principal = new StudentPrincipal(
                access.getTeacherStudentAccessId(), relation.getTeacherStudentId(), student.getStudentId());
        StudentLearningResponse learning = studentPortalService.learning(principal);
        assertThat(learning.curriculums()).hasSize(1);
        assertThat(learning.curriculums().get(0).categories())
                .extracting(view -> view.name(), view -> view.totalContentCount(), view -> view.contents().size())
                .containsExactly(tuple("A", 3, 2), tuple("B", 2, 0), tuple("C", 0, 0), tuple("상태", 1, 0));
        assertThat(learning.curriculums().get(0).categories().get(0).contents())
                .extracting(StudentContentView::name)
                .containsExactly("A1", "A2");
        assertThat(learning.curriculums().get(0).progress().completedCount()).isEqualTo(1);
        assertThat(learning.curriculums().get(0).progress().totalCount()).isEqualTo(6);
        String publishedNames = learning.curriculums().get(0).categories().stream()
                .flatMap(view -> view.contents().stream())
                .map(StudentContentView::name)
                .reduce("", (left, right) -> left + " " + right);
        assertThat(publishedNames).doesNotContain("A3", "B1", "B2", "사라진상세", "D1");
    }

    private Teacher teacher() {
        Teacher teacher = new Teacher();
        teacher.setEmail("portal.teacher." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        return teacher;
    }

    private Student student(String name) {
        Student student = new Student();
        student.setName(name);
        student.setStatus(RecordStatus.ACTIVE);
        return student;
    }

    private TeacherStudent relation(Long teacherId, Long studentId) {
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherId(teacherId);
        relation.setStudentId(studentId);
        relation.setStatus(RecordStatus.ACTIVE);
        return relation;
    }

    private Location location(Long teacherId) {
        Location location = new Location();
        location.setTeacherId(teacherId);
        location.setName("연습실");
        location.setDisplayOrder(1);
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

    private StudentMonitoring monitoring(Long studentCurriculumId, Long contentDetailId, ProgressStatus progressStatus) {
        StudentMonitoring studentMonitoring = new StudentMonitoring();
        studentMonitoring.setStudentCurriculumId(studentCurriculumId);
        studentMonitoring.setContentDetailId(contentDetailId);
        studentMonitoring.setDisplayOrder(1);
        studentMonitoring.setProgressStatus(progressStatus);
        studentMonitoring.setStatus(RecordStatus.ACTIVE);
        return studentMonitoring;
    }
}
