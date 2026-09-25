package com.yunki.lessonpt.student.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.dao.CannotAcquireLockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.dto.ActiveTeacherStudent;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.relationship.service.StudentAccessSessionService;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.dto.StudentCreateRequest;
import com.yunki.lessonpt.student.dto.StudentResponse;
import com.yunki.lessonpt.student.dto.StudentUpdateRequest;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private TeacherStudentMapper teacherStudentMapper;

    @Mock
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    @Mock
    private StudentCurriculumMapper studentCurriculumMapper;

    @Mock
    private TeacherStudentAccessMapper teacherStudentAccessMapper;

    @Mock
    private TeacherMapper teacherMapper;

    @Mock
    private StudentAccessSessionService studentAccessSessionService;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(
                studentMapper,
                teacherStudentMapper,
                teacherStudentLocationMapper,
                studentCurriculumMapper,
                teacherStudentAccessMapper,
                teacherMapper,
                studentAccessSessionService);
    }

    @Test
    void createWithoutEmailInsertsStudentAndRelation() {
        when(teacherMapper.selectActiveTeacherById(8L)).thenReturn(activeTeacher());
        when(studentMapper.insertStudent(any())).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setStudentId(40L);
            return 1;
        });
        when(teacherStudentMapper.insertTeacherStudent(any())).thenAnswer(invocation -> {
            TeacherStudent relation = invocation.getArgument(0);
            relation.setTeacherStudentId(70L);
            return 1;
        });
        when(teacherStudentMapper.selectActiveStudentForTeacher(8L, 40L)).thenReturn(link(70L, 40L, null, "김학생"));

        StudentResponse response = studentService.createStudent(8L, new StudentCreateRequest(null, "김학생", null));

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentMapper).insertStudent(studentCaptor.capture());
        assertThat(studentCaptor.getValue().getEmail()).isNull();
        assertThat(studentCaptor.getValue().getName()).isEqualTo("김학생");
        verify(teacherStudentMapper).insertTeacherStudent(any());
        assertThat(response.studentId()).isEqualTo(40L);
        assertThat(response.teacherStudentId()).isEqualTo(70L);
    }

    @Test
    void createNormalizesEmailBeforeInsert() {
        when(teacherMapper.selectActiveTeacherById(8L)).thenReturn(activeTeacher());
        when(studentMapper.selectStudentByEmail("student@lessonpt.local")).thenReturn(null);
        when(studentMapper.insertStudent(any())).thenAnswer(invocation -> {
            invocation.<Student>getArgument(0).setStudentId(41L);
            return 1;
        });
        when(teacherStudentMapper.insertTeacherStudent(any())).thenAnswer(invocation -> {
            invocation.<TeacherStudent>getArgument(0).setTeacherStudentId(71L);
            return 1;
        });
        when(teacherStudentMapper.selectActiveStudentForTeacher(8L, 41L))
                .thenReturn(link(71L, 41L, "student@lessonpt.local", "김학생"));

        studentService.createStudent(8L, new StudentCreateRequest("  Student@LessonPT.local ", "김학생", "010"));

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentMapper).insertStudent(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("student@lessonpt.local");
    }

    @Test
    void createReusesExistingStudentWithoutOverwritingProfile() {
        when(teacherMapper.selectActiveTeacherById(8L)).thenReturn(activeTeacher());
        Student existing = activeStudent(41L, "student@lessonpt.local", "기존이름");
        when(studentMapper.selectStudentByEmail("student@lessonpt.local")).thenReturn(existing);
        when(teacherStudentMapper.selectByTeacherIdAndStudentId(8L, 41L)).thenReturn(null);
        when(teacherStudentMapper.insertTeacherStudent(any())).thenAnswer(invocation -> {
            invocation.<TeacherStudent>getArgument(0).setTeacherStudentId(72L);
            return 1;
        });
        when(teacherStudentMapper.selectActiveStudentForTeacher(8L, 41L))
                .thenReturn(link(72L, 41L, "student@lessonpt.local", "기존이름"));

        StudentResponse response = studentService.createStudent(
                8L, new StudentCreateRequest("student@lessonpt.local", "다른이름", "0109999"));

        verify(studentMapper, never()).insertStudent(any());
        verify(studentMapper, never()).updateStudent(any());
        verify(teacherStudentMapper).insertTeacherStudent(any());
        assertThat(response.name()).isEqualTo("기존이름");
    }

    @Test
    void createRestoresInactiveRelation() {
        when(teacherMapper.selectActiveTeacherById(8L)).thenReturn(activeTeacher());
        when(studentMapper.selectStudentByEmail("student@lessonpt.local"))
                .thenReturn(activeStudent(41L, "student@lessonpt.local", "기존이름"));
        TeacherStudent inactive = relation(72L, RecordStatus.INACTIVE);
        when(teacherStudentMapper.selectByTeacherIdAndStudentId(8L, 41L)).thenReturn(inactive);
        when(teacherStudentMapper.lockTeacherStudentById(72L)).thenReturn(inactive);
        when(teacherStudentMapper.selectActiveStudentForTeacher(8L, 41L))
                .thenReturn(link(72L, 41L, "student@lessonpt.local", "기존이름"));

        studentService.createStudent(8L, new StudentCreateRequest("student@lessonpt.local", "다른이름", null));

        verify(teacherStudentMapper).restoreTeacherStudent(8L, 41L);
        verify(studentMapper, never()).updateStudent(any());
        verify(studentMapper, never()).restoreStudent(any());
    }

    @Test
    void createRejectsActiveRelation() {
        when(teacherMapper.selectActiveTeacherById(8L)).thenReturn(activeTeacher());
        when(studentMapper.selectStudentByEmail("student@lessonpt.local"))
                .thenReturn(activeStudent(41L, "student@lessonpt.local", "기존이름"));
        when(teacherStudentMapper.selectByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation(72L, RecordStatus.ACTIVE));

        assertThatThrownBy(() -> studentService.createStudent(
                8L, new StudentCreateRequest("student@lessonpt.local", "김학생", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
        verify(teacherStudentMapper, never()).insertTeacherStudent(any());
    }

    @Test
    void getReturnsOwnStudentAndHidesOthers() {
        when(teacherStudentMapper.selectActiveStudentForTeacher(8L, 41L))
                .thenReturn(link(72L, 41L, "student@lessonpt.local", "기존이름"));
        assertThat(studentService.getStudent(8L, 41L).teacherStudentId()).isEqualTo(72L);

        when(teacherStudentMapper.selectActiveStudentForTeacher(8L, 99L)).thenReturn(null);
        assertThatThrownBy(() -> studentService.getStudent(8L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void updateChangesNameClearsPhoneAndNormalizesEmail() {
        // Student 프로필은 강사별 복사본이 아니다. 이 수정은 같은 학생을 보는 다른 Teacher에게도 반영된다.
        when(teacherStudentMapper.selectActiveStudentForTeacher(8L, 41L))
                .thenReturn(link(72L, 41L, "new@lessonpt.local", "새이름"));
        Student student = activeStudent(41L, "old@lessonpt.local", "기존이름");
        student.setPhone("010");
        when(studentMapper.selectActiveStudentById(41L)).thenReturn(student);
        when(studentMapper.selectStudentByEmail("new@lessonpt.local")).thenReturn(null);
        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setName("새이름");
        request.setPhone(null);
        request.setEmail("  New@LessonPT.local ");

        studentService.updateStudent(8L, 41L, request);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentMapper).updateStudent(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("새이름");
        assertThat(captor.getValue().getPhone()).isNull();
        assertThat(captor.getValue().getEmail()).isEqualTo("new@lessonpt.local");
        verify(studentAccessSessionService).revokeActiveByStudentId(41L);
    }

    @Test
    void updateKeepsSessionsWhenEmailIsUnchangedAndRevokesWhenEmailIsRemoved() {
        when(teacherStudentMapper.selectActiveStudentForTeacher(8L, 41L))
                .thenReturn(link(72L, 41L, "same@lessonpt.local", "기존이름"));
        when(studentMapper.selectActiveStudentById(41L))
                .thenReturn(activeStudent(41L, "same@lessonpt.local", "기존이름"));
        StudentUpdateRequest same = new StudentUpdateRequest();
        same.setEmail(" Same@LessonPT.local ");
        studentService.updateStudent(8L, 41L, same);
        verify(studentAccessSessionService, never()).revokeActiveByStudentId(any());

        when(studentMapper.selectActiveStudentById(41L))
                .thenReturn(activeStudent(41L, "same@lessonpt.local", "기존이름"));
        StudentUpdateRequest cleared = new StudentUpdateRequest();
        cleared.setEmail(null);
        studentService.updateStudent(8L, 41L, cleared);
        verify(studentAccessSessionService).revokeActiveByStudentId(41L);
    }

    @Test
    void updateRejectsDuplicateEmailAndUnrelatedTeacher() {
        when(teacherStudentMapper.selectActiveStudentForTeacher(8L, 41L))
                .thenReturn(link(72L, 41L, "old@lessonpt.local", "기존이름"));
        when(studentMapper.selectActiveStudentById(41L)).thenReturn(activeStudent(41L, "old@lessonpt.local", "기존이름"));
        when(studentMapper.selectStudentByEmail("taken@lessonpt.local")).thenReturn(activeStudent(99L, "taken@lessonpt.local", "다른학생"));
        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setEmail("taken@lessonpt.local");

        assertThatThrownBy(() -> studentService.updateStudent(8L, 41L, request))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);

        when(teacherStudentMapper.selectActiveStudentForTeacher(9L, 41L)).thenReturn(null);
        assertThatThrownBy(() -> studentService.updateStudent(9L, 41L, request))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(studentMapper, never()).updateStudent(any());
    }

    @Test
    void releaseSoftDeletesRelationAndActiveChildrenOnly() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation(72L, RecordStatus.ACTIVE));
        when(teacherStudentMapper.lockTeacherStudentById(72L)).thenReturn(relation(72L, RecordStatus.ACTIVE));
        when(teacherStudentLocationMapper.selectActiveByTeacherStudentId(72L)).thenReturn(List.of());

        studentService.releaseStudent(8L, 41L);

        verify(studentAccessSessionService).revokeActiveByTeacherStudentId(72L);
        verify(teacherStudentAccessMapper).softDeleteActiveByTeacherStudentId(72L);
        verify(teacherStudentLocationMapper).softDeleteActiveByTeacherStudentId(72L);
        verify(teacherStudentMapper).softDeleteTeacherStudent(8L, 41L);
        verify(studentCurriculumMapper, never()).softDeleteActiveStudentCurriculumsByTeacherStudentLocationId(any());
        verify(studentMapper, never()).softDeleteStudent(any());
    }

    @Test
    void releaseSoftDeletesEachLocationLinkAndItsEnrollments() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation(72L, RecordStatus.ACTIVE));
        when(teacherStudentMapper.lockTeacherStudentById(72L)).thenReturn(relation(72L, RecordStatus.ACTIVE));
        when(teacherStudentLocationMapper.selectActiveByTeacherStudentId(72L))
                .thenReturn(List.of(studentLink(91L), studentLink(90L)));
        when(teacherStudentLocationMapper.lockTeacherStudentLocationById(90L)).thenReturn(studentLink(90L));
        when(teacherStudentLocationMapper.lockTeacherStudentLocationById(91L)).thenReturn(studentLink(91L));

        studentService.releaseStudent(8L, 41L);

        InOrder order = inOrder(
                teacherStudentMapper,
                studentAccessSessionService,
                teacherStudentAccessMapper,
                teacherStudentLocationMapper,
                studentCurriculumMapper);
        order.verify(teacherStudentMapper).lockTeacherStudentById(72L);
        order.verify(studentAccessSessionService).revokeActiveByTeacherStudentId(72L);
        order.verify(teacherStudentAccessMapper).softDeleteActiveByTeacherStudentId(72L);
        order.verify(teacherStudentLocationMapper).lockTeacherStudentLocationById(90L);
        order.verify(studentCurriculumMapper).softDeleteActiveStudentCurriculumsByTeacherStudentLocationId(90L);
        order.verify(teacherStudentLocationMapper).lockTeacherStudentLocationById(91L);
        order.verify(studentCurriculumMapper).softDeleteActiveStudentCurriculumsByTeacherStudentLocationId(91L);
        order.verify(teacherStudentLocationMapper).softDeleteActiveByTeacherStudentId(72L);
        order.verify(teacherStudentMapper).softDeleteTeacherStudent(8L, 41L);
        verify(studentMapper, never()).softDeleteStudent(any());
    }

    @Test
    void releaseStopsBeforeParentDeleteWhenEnrollmentCascadeFails() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation(72L, RecordStatus.ACTIVE));
        when(teacherStudentMapper.lockTeacherStudentById(72L)).thenReturn(relation(72L, RecordStatus.ACTIVE));
        when(teacherStudentLocationMapper.selectActiveByTeacherStudentId(72L)).thenReturn(List.of(studentLink(90L)));
        when(teacherStudentLocationMapper.lockTeacherStudentLocationById(90L)).thenReturn(studentLink(90L));
        doThrow(new IllegalStateException("cascade"))
                .when(studentCurriculumMapper).softDeleteActiveStudentCurriculumsByTeacherStudentLocationId(90L);

        assertThatThrownBy(() -> studentService.releaseStudent(8L, 41L))
                .isInstanceOf(IllegalStateException.class);
        verify(teacherStudentAccessMapper).softDeleteActiveByTeacherStudentId(72L);
        verify(teacherStudentLocationMapper, never()).softDeleteActiveByTeacherStudentId(any());
        verify(teacherStudentMapper, never()).softDeleteTeacherStudent(any(), any());
    }

    @Test
    void releaseStopsBeforeChildDeleteWhenAccessRevokeFails() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation(72L, RecordStatus.ACTIVE));
        when(teacherStudentMapper.lockTeacherStudentById(72L)).thenReturn(relation(72L, RecordStatus.ACTIVE));
        doThrow(new IllegalStateException("access"))
                .when(teacherStudentAccessMapper).softDeleteActiveByTeacherStudentId(72L);

        assertThatThrownBy(() -> studentService.releaseStudent(8L, 41L))
                .isInstanceOf(IllegalStateException.class);
        verify(teacherStudentLocationMapper, never()).selectActiveByTeacherStudentId(any());
        verify(teacherStudentMapper, never()).softDeleteTeacherStudent(any(), any());
    }

    @Test
    void releaseMapsLinkLockTimeoutToConflict() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation(72L, RecordStatus.ACTIVE));
        when(teacherStudentMapper.lockTeacherStudentById(72L)).thenReturn(relation(72L, RecordStatus.ACTIVE));
        when(teacherStudentLocationMapper.selectActiveByTeacherStudentId(72L)).thenReturn(List.of(studentLink(90L)));
        doThrow(new CannotAcquireLockException("ORA-30006"))
                .when(teacherStudentLocationMapper).lockTeacherStudentLocationById(90L);

        assertThatThrownBy(() -> studentService.releaseStudent(8L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
        verify(teacherStudentMapper, never()).softDeleteTeacherStudent(any(), any());
    }

    @Test
    void restoreRelationDoesNotRestoreChildLinks() {
        TeacherStudent inactive = relation(72L, RecordStatus.INACTIVE);
        when(teacherStudentMapper.selectByTeacherIdAndStudentId(8L, 41L)).thenReturn(inactive);
        when(studentMapper.selectStudentById(41L)).thenReturn(activeStudent(41L, null, "김학생"));
        when(teacherStudentMapper.lockTeacherStudentById(72L)).thenReturn(inactive);
        when(teacherStudentMapper.selectActiveStudentForTeacher(8L, 41L))
                .thenReturn(link(72L, 41L, null, "김학생"));

        studentService.restoreStudent(8L, 41L);

        verify(teacherStudentMapper).restoreTeacherStudent(8L, 41L);
        verify(teacherStudentLocationMapper, never()).restoreTeacherStudentLocation(any());
        verify(studentCurriculumMapper, never()).restoreStudentCurriculum(any());
    }

    @Test
    void restoreRelationDoesNotRestoreInactiveStudentOrActiveRelation() {
        TeacherStudent inactive = relation(72L, RecordStatus.INACTIVE);
        when(teacherStudentMapper.selectByTeacherIdAndStudentId(8L, 41L)).thenReturn(inactive);
        Student inactiveStudent = activeStudent(41L, null, "김학생");
        inactiveStudent.setStatus(RecordStatus.INACTIVE);
        when(studentMapper.selectStudentById(41L)).thenReturn(inactiveStudent);

        assertThatThrownBy(() -> studentService.restoreStudent(8L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
        verify(studentMapper, never()).restoreStudent(any());

        when(teacherStudentMapper.selectByTeacherIdAndStudentId(8L, 42L)).thenReturn(relation(73L, RecordStatus.ACTIVE));
        assertThatThrownBy(() -> studentService.restoreStudent(8L, 42L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);

        when(teacherStudentMapper.selectByTeacherIdAndStudentId(8L, 43L)).thenReturn(inactive);
        when(studentMapper.selectStudentById(43L)).thenReturn(activeStudent(43L, null, "김학생"));
        when(teacherStudentMapper.lockTeacherStudentById(72L)).thenReturn(inactive);
        when(teacherStudentMapper.selectActiveStudentForTeacher(8L, 43L)).thenReturn(link(72L, 43L, null, "김학생"));

        StudentResponse restored = studentService.restoreStudent(8L, 43L);

        verify(teacherStudentMapper).restoreTeacherStudent(8L, 43L);
        verify(studentMapper, never()).restoreStudent(any());
        assertThat(restored.studentId()).isEqualTo(43L);
    }

    private Teacher activeTeacher() {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(8L);
        teacher.setStatus(RecordStatus.ACTIVE);
        return teacher;
    }

    private Student activeStudent(Long studentId, String email, String name) {
        Student student = new Student();
        student.setStudentId(studentId);
        student.setEmail(email);
        student.setName(name);
        student.setStatus(RecordStatus.ACTIVE);
        return student;
    }

    private TeacherStudentLocation studentLink(Long id) {
        TeacherStudentLocation link = new TeacherStudentLocation();
        link.setTeacherStudentLocationId(id);
        link.setTeacherStudentId(72L);
        link.setStatus(RecordStatus.ACTIVE);
        return link;
    }

    private TeacherStudent relation(Long teacherStudentId, RecordStatus status) {
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherStudentId(teacherStudentId);
        relation.setTeacherId(8L);
        relation.setStatus(status);
        return relation;
    }

    private ActiveTeacherStudent link(Long teacherStudentId, Long studentId, String email, String name) {
        ActiveTeacherStudent linked = new ActiveTeacherStudent();
        linked.setTeacherStudentId(teacherStudentId);
        linked.setStudentId(studentId);
        linked.setEmail(email);
        linked.setName(name);
        return linked;
    }
}
