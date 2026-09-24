package com.yunki.lessonpt.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yunki.lessonpt.auth.domain.TeacherAuthSession;
import com.yunki.lessonpt.auth.dto.AuthTokenResponse;
import com.yunki.lessonpt.auth.jwt.IssuedToken;
import com.yunki.lessonpt.auth.jwt.JwtProperties;
import com.yunki.lessonpt.auth.jwt.JwtProvider;
import com.yunki.lessonpt.auth.jwt.TokenHasher;
import com.yunki.lessonpt.auth.mapper.TeacherAuthSessionMapper;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.dto.TeacherLoginRequest;
import com.yunki.lessonpt.teacher.dto.TeacherSignupRequest;
import com.yunki.lessonpt.teacher.dto.TeacherSignupResponse;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;
import com.yunki.lessonpt.teacher.service.TeacherService;

@ExtendWith(MockitoExtension.class)
class TeacherAuthServiceTest {

    private static final String RAW_PASSWORD = "Abcdef1!";

    @Mock
    private TeacherService teacherService;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private TeacherAuthSessionMapper sessionMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final TokenHasher tokenHasher = new TokenHasher();
    private JwtProvider jwtProvider;
    private TeacherAuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setAccessTokenTtl(Duration.ofMinutes(60));
        properties.setRefreshTokenTtl(Duration.ofDays(14));
        jwtProvider = new JwtProvider(properties);
        authService = new TeacherAuthService(
                teacherService,
                teacherMapper,
                sessionMapper,
                passwordEncoder,
                jwtProvider,
                tokenHasher);
    }

    @Test
    void loginIssuesTokenForActiveTeacher() {
        when(teacherMapper.selectActiveTeacherByEmail("teacher@lessonpt.local")).thenReturn(activeTeacher());
        when(teacherMapper.lockTeacherById(8L)).thenReturn(activeTeacher());

        var response = authService.login(new TeacherLoginRequest("teacher@lessonpt.local", RAW_PASSWORD));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.accessToken()).doesNotContain(RAW_PASSWORD);

        ArgumentCaptor<TeacherAuthSession> captor = ArgumentCaptor.forClass(TeacherAuthSession.class);
        verify(sessionMapper).insertAuthSession(captor.capture());
        TeacherAuthSession stored = captor.getValue();
        assertThat(stored.getTeacherId()).isEqualTo(8L);
        assertThat(stored.getAccessJti()).isEqualTo(jwtProvider.parseAccessToken(response.accessToken()).jti());
        assertThat(stored.getAccessJti()).isNotEqualTo(response.accessToken());
        assertThat(stored.getRefreshTokenHash()).isEqualTo(tokenHasher.hash(response.refreshToken()));
        assertThat(stored.getRefreshTokenHash()).isNotEqualTo(response.refreshToken());
        assertThat(stored.getAccessExpiresAt()).isNotNull();
        assertThat(stored.getRefreshExpiresAt()).isNotNull();
        assertThat(stored.getRefreshTokenHash()).doesNotContain(response.accessToken());
    }

    @Test
    void signupDelegatesCreationAndOmitsSecrets() {
        Teacher teacher = activeTeacher();
        teacher.setName("김강사");
        when(teacherService.createTeacher("teacher@lessonpt.local", RAW_PASSWORD, "김강사", null)).thenReturn(teacher);

        TeacherSignupResponse response = authService.signup(
                new TeacherSignupRequest("teacher@lessonpt.local", RAW_PASSWORD, "김강사", null));

        assertThat(response.teacherId()).isEqualTo(8L);
        assertThat(response.email()).isEqualTo("teacher@lessonpt.local");
        assertThat(response.name()).isEqualTo("김강사");
        assertThat(response.role()).isEqualTo("TEACHER");
        assertThat(response.email()).doesNotContain(RAW_PASSWORD);
        assertThat(response.name()).isNotEqualTo(teacher.getPasswordHash());
    }

    @Test
    void loginRejectsWrongPassword() {
        when(teacherMapper.selectActiveTeacherByEmail("teacher@lessonpt.local")).thenReturn(activeTeacher());

        assertThatThrownBy(() -> authService.login(new TeacherLoginRequest("teacher@lessonpt.local", "Wrong123!")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
        verify(sessionMapper, never()).insertAuthSession(any());
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(teacherMapper.selectActiveTeacherByEmail("missing@lessonpt.local")).thenReturn(null);

        assertThatThrownBy(() -> authService.login(new TeacherLoginRequest("missing@lessonpt.local", RAW_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
    }

    @Test
    void loginRejectsInactiveTeacher() {
        when(teacherMapper.selectActiveTeacherByEmail("inactive@lessonpt.local")).thenReturn(null);

        assertThatThrownBy(() -> authService.login(new TeacherLoginRequest("inactive@lessonpt.local", RAW_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
    }

    @Test
    void refreshKeepsRefreshTokenAndRotatesAccessJti() {
        IssuedToken refresh = jwtProvider.createRefreshToken(8L);
        TeacherAuthSession session = storedSession(refresh.value());
        when(sessionMapper.selectSessionByRefreshTokenHash(tokenHasher.hash(refresh.value()))).thenReturn(session);
        when(teacherMapper.selectActiveTeacherById(8L)).thenReturn(activeTeacher());

        AuthTokenResponse response = authService.refresh(refresh.value());

        assertThat(response.refreshToken()).isEqualTo(refresh.value());
        String newJti = jwtProvider.parseAccessToken(response.accessToken()).jti();
        assertThat(newJti).isNotEqualTo(refresh.jti());
        verify(sessionMapper).selectSessionByRefreshTokenHash(tokenHasher.hash(refresh.value()));
        verify(sessionMapper).updateAccessToken(eq(9L), eq(newJti), any());
    }

    @Test
    void refreshRejectsUnknownOrExpiredSession() {
        IssuedToken refresh = jwtProvider.createRefreshToken(8L);
        when(sessionMapper.selectSessionByRefreshTokenHash(tokenHasher.hash(refresh.value()))).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(refresh.value()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
        verify(sessionMapper, never()).updateAccessToken(any(), any(), any());
    }

    @Test
    void refreshRejectsExpiredToken() {
        IssuedToken expired = jwtProvider.createRefreshToken(8L, Duration.ofSeconds(-30));

        assertThatThrownBy(() -> authService.refresh(expired.value()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
        verify(sessionMapper, never()).selectSessionByRefreshTokenHash(any());
    }

    @Test
    void refreshRejectsInactiveTeacher() {
        IssuedToken refresh = jwtProvider.createRefreshToken(8L);
        when(sessionMapper.selectSessionByRefreshTokenHash(tokenHasher.hash(refresh.value())))
                .thenReturn(storedSession(refresh.value()));
        when(teacherMapper.selectActiveTeacherById(8L)).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(refresh.value()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
        verify(sessionMapper, never()).updateAccessToken(any(), any(), any());
    }

    @Test
    void logoutDeletesCurrentSession() {
        when(sessionMapper.selectActiveSessionByAccessJti("access-jti")).thenReturn(storedSession("refresh"));

        authService.logout("access-jti");

        verify(sessionMapper).deleteAuthSession(9L);
    }

    @Test
    void logoutIgnoresMissingSession() {
        when(sessionMapper.selectActiveSessionByAccessJti("missing")).thenReturn(null);

        authService.logout("missing");

        verify(sessionMapper, never()).deleteAuthSession(any());
        verify(sessionMapper, never()).deleteAuthSessionsByTeacherId(any());
    }

    private TeacherAuthSession storedSession(String refreshToken) {
        TeacherAuthSession session = new TeacherAuthSession();
        session.setAuthSessionId(9L);
        session.setTeacherId(8L);
        session.setRefreshTokenHash(tokenHasher.hash(refreshToken));
        return session;
    }

    private Teacher activeTeacher() {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(8L);
        teacher.setEmail("teacher@lessonpt.local");
        teacher.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        teacher.setStatus(RecordStatus.ACTIVE);
        teacher.setRole("TEACHER");
        return teacher;
    }
}
