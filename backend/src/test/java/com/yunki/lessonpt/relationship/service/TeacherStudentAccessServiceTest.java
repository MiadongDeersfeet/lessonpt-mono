package com.yunki.lessonpt.relationship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DuplicateKeyException;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentAccess;
import com.yunki.lessonpt.relationship.dto.TeacherStudentAccessResponse;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.relationship.service.StudentAccessSessionService;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;

@ExtendWith(MockitoExtension.class)
class TeacherStudentAccessServiceTest {

    @Mock
    private TeacherStudentMapper teacherStudentMapper;

    @Mock
    private TeacherStudentAccessMapper teacherStudentAccessMapper;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private StudentAccessSessionService studentAccessSessionService;

    private TeacherStudentAccessService service;

    @BeforeEach
    void setUp() {
        service = new TeacherStudentAccessService(
                teacherStudentMapper, teacherStudentAccessMapper, studentMapper, studentAccessSessionService);
    }

    @Test
    void createIssuesRandomKeyForActiveOwnedRelation() {
        stubOwnedActiveRelation();
        when(studentMapper.selectActiveStudentById(41L)).thenReturn(student("student@lessonpt.local"));
        when(teacherStudentAccessMapper.selectByTeacherStudentId(72L)).thenReturn(null);
        when(teacherStudentAccessMapper.insertTeacherStudentAccess(any())).thenReturn(1);
        when(teacherStudentAccessMapper.selectActiveByTeacherStudentId(72L)).thenReturn(stored("key-from-db"));

        TeacherStudentAccessResponse response = service.createAccess(8L, 41L);

        ArgumentCaptor<TeacherStudentAccess> captor = ArgumentCaptor.forClass(TeacherStudentAccess.class);
        InOrder order = inOrder(teacherStudentMapper, studentMapper, teacherStudentAccessMapper);
        order.verify(teacherStudentMapper).lockTeacherStudentById(72L);
        order.verify(studentMapper).selectActiveStudentById(41L);
        order.verify(teacherStudentAccessMapper).selectByTeacherStudentId(72L);
        order.verify(teacherStudentAccessMapper).insertTeacherStudentAccess(captor.capture());
        TeacherStudentAccess inserted = captor.getValue();
        assertThat(inserted.getTeacherStudentId()).isEqualTo(72L);
        assertThat(inserted.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(inserted.getPublicAccessKey()).isNotEqualTo("72");
        assertThat(UUID.fromString(inserted.getPublicAccessKey())).isNotNull();
        assertThat(response.teacherStudentAccessId()).isEqualTo(90L);
        assertThat(response.teacherStudentId()).isEqualTo(72L);
        assertThat(response.publicAccessKey()).isEqualTo("key-from-db");
        assertThat(response.status()).isEqualTo(RecordStatus.ACTIVE);
    }

    @Test
    void createHidesAnotherTeachersRelationAndInactiveRelation() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(9L, 41L)).thenReturn(null);
        assertThatThrownBy(() -> service.createAccess(9L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);

        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(null);
        assertThatThrownBy(() -> service.createAccess(8L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(teacherStudentAccessMapper, never()).insertTeacherStudentAccess(any());
    }

    @Test
    void createRejectsStudentWithoutEmailAndExistingAccess() {
        stubOwnedActiveRelation();
        when(studentMapper.selectActiveStudentById(41L)).thenReturn(student(null));
        assertThatThrownBy(() -> service.createAccess(8L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);

        when(studentMapper.selectActiveStudentById(41L)).thenReturn(student("student@lessonpt.local"));
        when(teacherStudentAccessMapper.selectByTeacherStudentId(72L)).thenReturn(stored("existing"));
        assertThatThrownBy(() -> service.createAccess(8L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
        verify(teacherStudentAccessMapper, never()).insertTeacherStudentAccess(any());
        verify(teacherStudentAccessMapper, never()).reactivateTeacherStudentAccess(any());
    }

    @Test
    void createReactivatesRevokedAccessWithANewKey() {
        stubOwnedActiveRelation();
        when(studentMapper.selectActiveStudentById(41L)).thenReturn(student("student@lessonpt.local"));
        TeacherStudentAccess revoked = stored("old-key");
        revoked.setStatus(RecordStatus.INACTIVE);
        when(teacherStudentAccessMapper.selectByTeacherStudentId(72L)).thenReturn(revoked);
        when(teacherStudentAccessMapper.reactivateTeacherStudentAccess(revoked)).thenReturn(1);
        TeacherStudentAccess reloaded = stored("new-key");
        when(teacherStudentAccessMapper.selectActiveByTeacherStudentId(72L)).thenReturn(reloaded);
        when(teacherStudentAccessMapper.selectByPublicAccessKey("old-key")).thenReturn(null);
        when(teacherStudentAccessMapper.selectByPublicAccessKey("new-key")).thenReturn(reloaded);

        TeacherStudentAccessResponse response = service.createAccess(8L, 41L);

        assertThat(revoked.getTeacherStudentAccessId()).isEqualTo(90L);
        assertThat(revoked.getPublicAccessKey()).isNotEqualTo("old-key");
        assertThat(UUID.fromString(revoked.getPublicAccessKey())).isNotNull();
        assertThat(response.teacherStudentAccessId()).isEqualTo(90L);
        assertThat(response.publicAccessKey()).isEqualTo("new-key");
        assertThat(response.status()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(teacherStudentAccessMapper.selectByPublicAccessKey("old-key")).isNull();
        assertThat(teacherStudentAccessMapper.selectByPublicAccessKey("new-key").getStatus()).isEqualTo(RecordStatus.ACTIVE);
        verify(teacherStudentAccessMapper, never()).insertTeacherStudentAccess(any());

        when(teacherStudentAccessMapper.selectByTeacherStudentId(72L)).thenReturn(stored("new-key"));
        assertThatThrownBy(() -> service.createAccess(8L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
    }

    @Test
    void createDoesNotReactivateWithoutEmailOrWhenRelationIsInactive() {
        stubOwnedActiveRelation();
        when(studentMapper.selectActiveStudentById(41L)).thenReturn(student(" "));
        assertThatThrownBy(() -> service.createAccess(8L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);

        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(null);
        assertThatThrownBy(() -> service.createAccess(8L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(teacherStudentAccessMapper, never()).reactivateTeacherStudentAccess(any());
    }

    @Test
    void createMapsPublicKeyCollisionToConflict() {
        stubOwnedActiveRelation();
        when(studentMapper.selectActiveStudentById(41L)).thenReturn(student("student@lessonpt.local"));
        when(teacherStudentAccessMapper.selectByTeacherStudentId(72L)).thenReturn(null);
        when(teacherStudentAccessMapper.insertTeacherStudentAccess(any()))
                .thenThrow(new DuplicateKeyException("UK_TSA_PUBLIC_KEY"));

        assertThatThrownBy(() -> service.createAccess(8L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
    }

    @Test
    void createMapsLockTimeoutToConflict() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation());
        doThrow(new CannotAcquireLockException("ORA-30006")).when(teacherStudentMapper).lockTeacherStudentById(72L);

        assertThatThrownBy(() -> service.createAccess(8L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
        verify(teacherStudentAccessMapper, never()).insertTeacherStudentAccess(any());
    }

    @Test
    void getReturnsActiveAccessAndHidesOtherTeachers() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation());
        when(teacherStudentAccessMapper.selectActiveByTeacherStudentId(72L)).thenReturn(stored("key"));

        TeacherStudentAccessResponse response = service.getAccess(8L, 41L);

        assertThat(response.publicAccessKey()).isEqualTo("key");
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(9L, 41L)).thenReturn(null);
        assertThatThrownBy(() -> service.getAccess(9L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void revokeSoftDeletesOneActiveAccess() {
        stubOwnedActiveRelation();
        when(teacherStudentAccessMapper.selectActiveByTeacherStudentId(72L)).thenReturn(stored("key"));
        when(teacherStudentAccessMapper.lockTeacherStudentAccessById(90L)).thenReturn(stored("key"));
        when(teacherStudentAccessMapper.softDeleteActiveByTeacherStudentId(72L)).thenReturn(1);

        service.revokeAccess(8L, 41L);

        InOrder order = inOrder(teacherStudentMapper, teacherStudentAccessMapper, studentAccessSessionService);
        order.verify(teacherStudentMapper).lockTeacherStudentById(72L);
        order.verify(teacherStudentAccessMapper).lockTeacherStudentAccessById(90L);
        order.verify(studentAccessSessionService).revokeActiveByAccessId(90L);
        order.verify(teacherStudentAccessMapper).softDeleteActiveByTeacherStudentId(72L);
    }

    @Test
    void revokeReportsMissingAccessAsNotFound() {
        stubOwnedActiveRelation();
        when(teacherStudentAccessMapper.selectActiveByTeacherStudentId(72L)).thenReturn(null);

        assertThatThrownBy(() -> service.revokeAccess(8L, 41L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    private void stubOwnedActiveRelation() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation());
        when(teacherStudentMapper.lockTeacherStudentById(72L)).thenReturn(relation());
    }

    private TeacherStudent relation() {
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherStudentId(72L);
        relation.setTeacherId(8L);
        relation.setStudentId(41L);
        relation.setStatus(RecordStatus.ACTIVE);
        return relation;
    }

    private Student student(String email) {
        Student student = new Student();
        student.setStudentId(41L);
        student.setEmail(email);
        student.setStatus(RecordStatus.ACTIVE);
        return student;
    }

    private TeacherStudentAccess stored(String publicAccessKey) {
        TeacherStudentAccess access = new TeacherStudentAccess();
        access.setTeacherStudentAccessId(90L);
        access.setTeacherStudentId(72L);
        access.setPublicAccessKey(publicAccessKey);
        access.setCreatedAt(LocalDateTime.of(2026, 9, 24, 12, 0));
        access.setStatus(RecordStatus.ACTIVE);
        return access;
    }
}
