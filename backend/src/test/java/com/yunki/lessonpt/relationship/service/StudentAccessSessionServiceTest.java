package com.yunki.lessonpt.relationship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.yunki.lessonpt.auth.jwt.TokenHasher;
import com.yunki.lessonpt.auth.security.StudentPrincipal;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.StudentAccessSession;
import com.yunki.lessonpt.relationship.domain.StudentAccessSessionStatus;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentAccess;
import com.yunki.lessonpt.relationship.mapper.StudentAccessSessionMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;

@ExtendWith(MockitoExtension.class)
class StudentAccessSessionServiceTest {

    private static final Instant START = Instant.parse("2026-09-24T00:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 24, 0, 0);

    @Mock
    private StudentAccessSessionMapper studentAccessSessionMapper;
    @Mock
    private TeacherStudentAccessMapper teacherStudentAccessMapper;
    @Mock
    private TeacherStudentMapper teacherStudentMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private StudentSessionTokenGenerator tokenGenerator;

    private final TokenHasher tokenHasher = new TokenHasher();
    private StudentAccessSessionService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(START);
    }

    @Test
    void openStoresHashOnlyAndRevokesPreviousActiveSession() {
        when(tokenGenerator.generate()).thenReturn("raw-session-token-value");
        when(teacherStudentAccessMapper.lockTeacherStudentAccessById(90L)).thenReturn(access());
        when(teacherStudentMapper.selectTeacherStudentById(72L)).thenReturn(relation());
        when(studentAccessSessionMapper.insertStudentAccessSession(any())).thenReturn(1);
        when(teacherStudentAccessMapper.updateLastVerifiedAt(90L, NOW)).thenReturn(1);

        IssuedStudentSession issued = service.openAfterOtp(90L);

        assertThat(issued.rawToken()).isEqualTo("raw-session-token-value");
        assertThat(issued.expiresAt()).isEqualTo(NOW.plusDays(30));
        assertThat(issued.absoluteExpiresAt()).isEqualTo(NOW.plusDays(180));
        ArgumentCaptor<StudentAccessSession> captor = ArgumentCaptor.forClass(StudentAccessSession.class);
        verify(studentAccessSessionMapper).revokeActiveByAccessId(90L, NOW, NOW);
        verify(studentAccessSessionMapper).insertStudentAccessSession(captor.capture());
        StudentAccessSession stored = captor.getValue();
        assertThat(stored.getSessionTokenHash()).isEqualTo(tokenHasher.hash("raw-session-token-value"));
        assertThat(stored.getSessionTokenHash()).isNotEqualTo("raw-session-token-value");
        assertThat(stored.getSessionStatus()).isEqualTo(StudentAccessSessionStatus.ACTIVE);
        assertThat(stored.getRevokedAt()).isNull();
        assertThat(stored.getLastAccessedAt()).isEqualTo(NOW);
        assertThat(stored.getStudentId()).isEqualTo(41L);
        assertThat(stored.getTeacherStudentAccessId()).isEqualTo(90L);
    }

    @Test
    void loginSessionStartsWithoutScopeAndCanSelectOnlyOwnActiveAccess() {
        when(tokenGenerator.generate()).thenReturn("identity-token");
        when(studentAccessSessionMapper.insertStudentAccessSession(any())).thenReturn(1);
        IssuedStudentSession issued = service.openForStudent(41L);
        ArgumentCaptor<StudentAccessSession> created = ArgumentCaptor.forClass(StudentAccessSession.class);
        verify(studentAccessSessionMapper).insertStudentAccessSession(created.capture());
        assertThat(created.getValue().getStudentId()).isEqualTo(41L);
        assertThat(created.getValue().getTeacherStudentAccessId()).isNull();
        assertThat(issued.rawToken()).isEqualTo("identity-token");

        StudentAccessSession stored = activeSession(NOW.plusDays(20));
        stored.setTeacherStudentAccessId(null);
        when(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash("identity-token"))).thenReturn(stored);
        when(studentMapper.selectActiveStudentById(41L)).thenReturn(student());
        StudentPrincipal principal = service.authenticate("identity-token").orElseThrow();
        assertThat(principal.studentId()).isEqualTo(41L);
        assertThat(principal.teacherStudentAccessId()).isNull();

        when(teacherStudentAccessMapper.lockTeacherStudentAccessById(90L)).thenReturn(access());
        when(teacherStudentMapper.selectTeacherStudentById(72L)).thenReturn(relation());
        when(studentAccessSessionMapper.updateSelectedAccess(any())).thenReturn(1);
        service.selectScope("identity-token", 90L);
        ArgumentCaptor<StudentAccessSession> scoped = ArgumentCaptor.forClass(StudentAccessSession.class);
        verify(studentAccessSessionMapper).updateSelectedAccess(scoped.capture());
        assertThat(scoped.getValue().getTeacherStudentAccessId()).isEqualTo(90L);
        assertThat(scoped.getValue().getStudentId()).isEqualTo(41L);
    }

    @Test
    void selectScopeRejectsAnotherStudentAndInactiveAccess() {
        StudentAccessSession stored = activeSession(NOW.plusDays(20));
        stored.setTeacherStudentAccessId(null);
        when(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash("identity-token"))).thenReturn(stored);
        TeacherStudent other = relation();
        other.setStudentId(99L);
        Student otherStudent = student();
        otherStudent.setStudentId(99L);
        when(teacherStudentAccessMapper.lockTeacherStudentAccessById(91L)).thenReturn(access());
        when(teacherStudentMapper.selectTeacherStudentById(72L)).thenReturn(other);
        when(studentMapper.selectActiveStudentById(99L)).thenReturn(otherStudent);
        assertThatThrownBy(() -> service.selectScope("identity-token", 91L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);

        when(teacherStudentAccessMapper.lockTeacherStudentAccessById(90L)).thenReturn(revokedAccess());
        assertThatThrownBy(() -> service.selectScope("identity-token", 90L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(studentAccessSessionMapper, never()).updateSelectedAccess(any());
    }

    @Test
    void clearScopeKeepsTheSessionAndLogoutRevokesOnlyThatRow() {
        StudentAccessSession stored = activeSession(NOW.plusDays(20));
        when(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash("token"))).thenReturn(stored);
        when(studentAccessSessionMapper.updateSelectedAccess(any())).thenReturn(1);
        service.clearScope("token");
        ArgumentCaptor<StudentAccessSession> cleared = ArgumentCaptor.forClass(StudentAccessSession.class);
        verify(studentAccessSessionMapper).updateSelectedAccess(cleared.capture());
        assertThat(cleared.getValue().getTeacherStudentAccessId()).isNull();
        assertThat(cleared.getValue().getStudentAccessSessionId()).isEqualTo(5L);

        service.logout("token");
        verify(studentAccessSessionMapper).revokeBySessionId(5L, NOW, NOW);
        verify(studentAccessSessionMapper, never()).revokeActiveByAccessId(any(), any(), any());
    }

    @Test
    void openMapsTokenCollisionToConflict() {
        when(tokenGenerator.generate()).thenReturn("raw-session-token-value");
        when(teacherStudentAccessMapper.lockTeacherStudentAccessById(90L)).thenReturn(access());
        when(teacherStudentMapper.selectTeacherStudentById(72L)).thenReturn(relation());
        when(studentAccessSessionMapper.insertStudentAccessSession(any()))
                .thenThrow(new DuplicateKeyException("UK_SAS_TOKEN_HASH"));

        assertThatThrownBy(() -> service.openAfterOtp(90L))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
    }

    @Test
    void authenticateRejectsMissingRevokedAndExpiredSessions() {
        assertThat(service.authenticate(null)).isEmpty();
        assertThat(service.authenticate("missing")).isEmpty();

        when(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash("revoked"))).thenReturn(session(
                StudentAccessSessionStatus.REVOKED, NOW.plusDays(30), NOW.plusDays(180), NOW));
        assertThat(service.authenticate("revoked")).isEmpty();

        when(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash("idle"))).thenReturn(session(
                StudentAccessSessionStatus.ACTIVE, NOW, NOW.plusDays(180), NOW.minusDays(30)));
        assertThat(service.authenticate("idle")).isEmpty();

        when(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash("absolute"))).thenReturn(session(
                StudentAccessSessionStatus.ACTIVE, NOW.plusDays(1), NOW, NOW.minusDays(1)));
        assertThat(service.authenticate("absolute")).isEmpty();
        verify(studentAccessSessionMapper, never()).updateSlidingWindow(any());
    }

    @Test
    void authenticateRejectsInactiveAccessRelationAndStudent() {
        when(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash("token"))).thenReturn(activeSession(NOW.plusDays(20)));
        when(studentMapper.selectActiveStudentById(41L)).thenReturn(student(), student(), student(), null);
        when(teacherStudentAccessMapper.lockTeacherStudentAccessById(90L)).thenReturn(revokedAccess());
        assertThat(service.authenticate("token")).isEmpty();

        when(teacherStudentAccessMapper.lockTeacherStudentAccessById(90L)).thenReturn(access());
        when(teacherStudentMapper.selectTeacherStudentById(72L)).thenReturn(null);
        assertThat(service.authenticate("token")).isEmpty();

        TeacherStudent released = relation();
        released.setStatus(RecordStatus.INACTIVE);
        when(teacherStudentMapper.selectTeacherStudentById(72L)).thenReturn(released);
        assertThat(service.authenticate("token")).isEmpty();

        assertThat(service.authenticate("token")).isEmpty();
    }

    @Test
    void slidingExtendsOnlyWithinSevenDaysAndTouchesLastAccessAfterTwentyFourHours() {
        when(teacherStudentAccessMapper.lockTeacherStudentAccessById(90L)).thenReturn(access());
        when(teacherStudentMapper.selectTeacherStudentById(72L)).thenReturn(relation());
        when(studentMapper.selectActiveStudentById(41L)).thenReturn(student());

        StudentAccessSession eightDays = activeSession(NOW.plusDays(8));
        eightDays.setLastAccessedAt(NOW.minusHours(23).minusMinutes(59));
        when(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash("keep"))).thenReturn(eightDays);
        assertThat(service.authenticate("keep")).isPresent();
        verify(studentAccessSessionMapper, never()).updateSlidingWindow(any());

        StudentAccessSession sevenDays = activeSession(NOW.plusDays(7));
        sevenDays.setLastAccessedAt(NOW.minusHours(23).minusMinutes(59));
        when(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash("extend"))).thenReturn(sevenDays);
        serviceAt(START).authenticate("extend");
        ArgumentCaptor<StudentAccessSession> extended = ArgumentCaptor.forClass(StudentAccessSession.class);
        verify(studentAccessSessionMapper).updateSlidingWindow(extended.capture());
        assertThat(extended.getValue().getExpiresAt()).isEqualTo(NOW.plusDays(30));
        assertThat(extended.getValue().getLastAccessedAt()).isEqualTo(NOW.minusHours(23).minusMinutes(59));

        StudentAccessSession nearAbsolute = activeSession(NOW.plusDays(1));
        nearAbsolute.setAbsoluteExpiresAt(NOW.plusDays(10));
        nearAbsolute.setLastAccessedAt(NOW.minusHours(24));
        when(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash("cap"))).thenReturn(nearAbsolute);
        service.authenticate("cap");
        ArgumentCaptor<StudentAccessSession> capped = ArgumentCaptor.forClass(StudentAccessSession.class);
        verify(studentAccessSessionMapper, org.mockito.Mockito.times(2)).updateSlidingWindow(capped.capture());
        StudentAccessSession saved = capped.getAllValues().get(1);
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plusDays(10));
        assertThat(saved.getLastAccessedAt()).isEqualTo(NOW);
        assertThat(saved.getExpiresAt()).isBeforeOrEqualTo(saved.getAbsoluteExpiresAt());
    }

    @Test
    void successfulAuthenticationBuildsPrincipal() {
        when(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash("token"))).thenReturn(activeSession(NOW.plusDays(20)));
        when(teacherStudentAccessMapper.lockTeacherStudentAccessById(90L)).thenReturn(access());
        when(teacherStudentMapper.selectTeacherStudentById(72L)).thenReturn(relation());
        when(studentMapper.selectActiveStudentById(41L)).thenReturn(student());

        StudentPrincipal principal = service.authenticate("token").orElseThrow();

        assertThat(principal.teacherStudentAccessId()).isEqualTo(90L);
        assertThat(principal.teacherStudentId()).isEqualTo(72L);
        assertThat(principal.studentId()).isEqualTo(41L);
    }

    private StudentAccessSessionService serviceAt(Instant instant) {
        return new StudentAccessSessionService(
                studentAccessSessionMapper,
                teacherStudentAccessMapper,
                teacherStudentMapper,
                studentMapper,
                tokenGenerator,
                tokenHasher,
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    private StudentAccessSession activeSession(LocalDateTime expiresAt) {
        return session(StudentAccessSessionStatus.ACTIVE, expiresAt, NOW.plusDays(180), NOW);
    }

    private StudentAccessSession session(
            StudentAccessSessionStatus status,
            LocalDateTime expiresAt,
            LocalDateTime absoluteExpiresAt,
            LocalDateTime lastAccessedAt) {
        StudentAccessSession session = new StudentAccessSession();
        session.setStudentAccessSessionId(5L);
        session.setStudentId(41L);
        session.setTeacherStudentAccessId(90L);
        session.setSessionStatus(status);
        session.setExpiresAt(expiresAt);
        session.setAbsoluteExpiresAt(absoluteExpiresAt);
        session.setLastAccessedAt(lastAccessedAt);
        session.setCreatedAt(NOW.minusDays(1));
        return session;
    }

    private TeacherStudentAccess access() {
        TeacherStudentAccess access = new TeacherStudentAccess();
        access.setTeacherStudentAccessId(90L);
        access.setTeacherStudentId(72L);
        access.setStatus(RecordStatus.ACTIVE);
        return access;
    }

    private TeacherStudentAccess revokedAccess() {
        TeacherStudentAccess access = access();
        access.setStatus(RecordStatus.INACTIVE);
        access.setRevokedAt(NOW);
        return access;
    }

    private TeacherStudent relation() {
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherStudentId(72L);
        relation.setStudentId(41L);
        relation.setStatus(RecordStatus.ACTIVE);
        return relation;
    }

    private Student student() {
        Student student = new Student();
        student.setStudentId(41L);
        student.setStatus(RecordStatus.ACTIVE);
        return student;
    }
}
