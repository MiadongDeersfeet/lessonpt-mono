package com.yunki.lessonpt.auth.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.yunki.lessonpt.auth.domain.TeacherAuthSession;
import com.yunki.lessonpt.auth.jwt.JwtProvider;
import com.yunki.lessonpt.auth.jwt.JwtVerificationException;
import com.yunki.lessonpt.auth.jwt.VerifiedToken;
import com.yunki.lessonpt.auth.mapper.TeacherAuthSessionMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 서명이 맞아도 세션 행이 없으면 로그인된 것으로 보지 않는다.
 * 로그아웃으로 지운 jti는 만료 전이라도 다시 쓸 수 없게 한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectProvider<TeacherMapper> teacherMapper;
    private final ObjectProvider<TeacherAuthSessionMapper> sessionMapper;
    private final JwtProvider jwtProvider;
    private final AuthErrorWriter authErrorWriter;

    public JwtAuthenticationFilter(
            ObjectProvider<TeacherMapper> teacherMapper,
            ObjectProvider<TeacherAuthSessionMapper> sessionMapper,
            JwtProvider jwtProvider,
            AuthErrorWriter authErrorWriter) {
        this.teacherMapper = teacherMapper;
        this.sessionMapper = sessionMapper;
        this.jwtProvider = jwtProvider;
        this.authErrorWriter = authErrorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring("Bearer ".length()).trim();
        try {
            VerifiedToken verified = jwtProvider.parseAccessToken(token);
            if (!registerPrincipal(verified)) {
                authErrorWriter.writeUnauthorized(request, response);
                return;
            }
            filterChain.doFilter(request, response);
        } catch (JwtVerificationException | IllegalStateException exception) {
            SecurityContextHolder.clearContext();
            authErrorWriter.writeUnauthorized(request, response);
        }
    }

    private boolean registerPrincipal(VerifiedToken verified) {
        TeacherAuthSessionMapper sessions = sessionMapper.getIfAvailable();
        TeacherMapper teachers = teacherMapper.getIfAvailable();
        if (sessions == null || teachers == null || verified.jti() == null) {
            return false;
        }
        TeacherAuthSession session = sessions.selectActiveSessionByAccessJti(verified.jti());
        if (session == null || !verified.teacherId().equals(session.getTeacherId())) {
            return false;
        }
        Teacher teacher = teachers.selectActiveTeacherById(verified.teacherId());
        if (teacher == null) {
            return false;
        }
        TeacherPrincipal principal = new TeacherPrincipal(teacher.getTeacherId(), teacher.getEmail());
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(authority(teacher.getRole()))));
        authentication.setDetails(verified.jti());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        sessions.updateLastUsedAt(session.getAuthSessionId());
        return true;
    }

    private String authority(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_TEACHER";
        }
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
