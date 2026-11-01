package com.yunki.lessonpt.auth.security;

import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Reversible pause: no OTP issuance, portal reads, or new teacher invitations. */
public class StudentPortalAvailabilityFilter extends OncePerRequestFilter {
    private final boolean enabled;

    public StudentPortalAvailabilityFilter(boolean enabled) { this.enabled = enabled; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean studentPath = path.equals("/api/v1/student") || path.startsWith("/api/v1/student/")
                || path.equals("/api/v1/student-login") || path.startsWith("/api/v1/student-login/")
                || path.equals("/api/v1/student-access") || path.startsWith("/api/v1/student-access/");
        boolean newInvitation = "POST".equals(request.getMethod()) && path.matches("/api/v1/students/[^/]+/access/?");
        boolean logout = "POST".equals(request.getMethod()) && path.equals("/api/v1/student/session/logout");
        if (!enabled && !"OPTIONS".equals(request.getMethod()) && !logout && (studentPath || newInvitation)) {
            response.setStatus(503);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write("{\"status\":503,\"code\":\"STUDENT_PORTAL_PAUSED\",\"message\":\"학생 서비스는 잠정 중단되었습니다.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
