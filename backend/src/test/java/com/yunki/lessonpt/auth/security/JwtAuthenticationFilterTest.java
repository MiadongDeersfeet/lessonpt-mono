package com.yunki.lessonpt.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.yunki.lessonpt.auth.domain.TeacherAuthSession;
import com.yunki.lessonpt.auth.jwt.IssuedToken;
import com.yunki.lessonpt.auth.jwt.JwtProperties;
import com.yunki.lessonpt.auth.jwt.JwtProvider;
import com.yunki.lessonpt.auth.mapper.TeacherAuthSessionMapper;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

import jakarta.servlet.FilterChain;

class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider = provider();
    private TeacherMapper teacherMapper;
    private TeacherAuthSessionMapper sessionMapper;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        teacherMapper = mock(TeacherMapper.class);
        sessionMapper = mock(TeacherAuthSessionMapper.class);
        filter = new JwtAuthenticationFilter(
                providerOf(teacherMapper),
                providerOf(sessionMapper),
                jwtProvider,
                mock(AuthErrorWriter.class));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registersTeacherPrincipalWhenSessionExists() throws Exception {
        IssuedToken token = jwtProvider.createAccessToken(21L);
        Teacher teacher = teacher(21L);
        when(sessionMapper.selectActiveSessionByAccessJti(token.jti())).thenReturn(session(21L, token.jti()));
        when(teacherMapper.selectActiveTeacherById(21L)).thenReturn(teacher);
        MockHttpServletRequest request = request(token.value());
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        TeacherPrincipal principal = (TeacherPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.teacherId()).isEqualTo(21L);
        assertThat(principal.email()).isEqualTo("teacher@lessonpt.local");
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(sessionMapper).updateLastUsedAt(3L);
    }

    @Test
    void rejectsActiveSessionWhenTeacherIsInactive() throws Exception {
        IssuedToken token = jwtProvider.createAccessToken(21L);
        when(sessionMapper.selectActiveSessionByAccessJti(token.jti())).thenReturn(session(21L, token.jti()));
        when(teacherMapper.selectActiveTeacherById(21L)).thenReturn(null);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request(token.value()), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(sessionMapper, never()).updateLastUsedAt(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsTokenWhenSessionRowIsMissing() throws Exception {
        IssuedToken token = jwtProvider.createAccessToken(21L);
        when(sessionMapper.selectActiveSessionByAccessJti(token.jti())).thenReturn(null);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request(token.value()), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsTamperedAndExpiredTokens() throws Exception {
        IssuedToken token = jwtProvider.createAccessToken(21L);
        String tampered = token.value().substring(0, token.value().length() - 2) + "aa";
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request(tampered), new MockHttpServletResponse(), chain);

        IssuedToken expired = jwtProvider.createAccessToken(21L, Duration.ofSeconds(-30));
        filter.doFilter(request(expired.value()), new MockHttpServletResponse(), chain);

        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(sessionMapper, never()).selectActiveSessionByAccessJti(org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private Teacher teacher(Long teacherId) {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(teacherId);
        teacher.setEmail("teacher@lessonpt.local");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        return teacher;
    }

    private TeacherAuthSession session(Long teacherId, String jti) {
        TeacherAuthSession session = new TeacherAuthSession();
        session.setAuthSessionId(3L);
        session.setTeacherId(teacherId);
        session.setAccessJti(jti);
        return session;
    }

    private JwtProvider provider() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setAccessTokenTtl(Duration.ofMinutes(60));
        properties.setRefreshTokenTtl(Duration.ofDays(14));
        return new JwtProvider(properties);
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> providerOf(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
