package com.yunki.lessonpt.auth.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.yunki.lessonpt.relationship.config.StudentSessionProperties;
import com.yunki.lessonpt.relationship.service.StudentAccessSessionService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StudentSessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String STUDENT_AUTHORITY = "ROLE_STUDENT";

    private final StudentAccessSessionService studentAccessSessionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !(path.equals(StudentSessionProperties.COOKIE_PATH)
                || path.startsWith(StudentSessionProperties.COOKIE_PATH + "/"))) {
            return true;
        }
        return HttpMethod.POST.matches(request.getMethod()) && path.equals("/api/v1/student/session/logout");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawToken = readCookie(request);
        try {
            studentAccessSessionService.authenticate(rawToken).ifPresent(this::setPrincipal);
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

    private void setPrincipal(StudentPrincipal principal) {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority(STUDENT_AUTHORITY)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (StudentSessionProperties.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
