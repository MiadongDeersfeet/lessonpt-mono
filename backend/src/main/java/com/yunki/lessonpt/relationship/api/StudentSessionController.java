package com.yunki.lessonpt.relationship.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yunki.lessonpt.student.dto.StudentScopeRequest;

import jakarta.validation.Valid;

import com.yunki.lessonpt.relationship.config.StudentSessionProperties;
import com.yunki.lessonpt.relationship.service.StudentAccessSessionService;
import com.yunki.lessonpt.relationship.service.StudentSessionCookieWriter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/student/session")
@RequiredArgsConstructor
public class StudentSessionController {

    private final StudentAccessSessionService studentAccessSessionService;
    private final StudentSessionCookieWriter studentSessionCookieWriter;

    @PostMapping("/scope")
    public ResponseEntity<Void> selectScope(
            HttpServletRequest request,
            @Valid @RequestBody StudentScopeRequest body) {
        studentAccessSessionService.selectScope(readCookie(request), body.teacherStudentAccessId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/scope")
    public ResponseEntity<Void> clearScope(HttpServletRequest request) {
        studentAccessSessionService.clearScope(readCookie(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        studentAccessSessionService.logout(readCookie(request));
        studentSessionCookieWriter.expire(response);
        return ResponseEntity.noContent().build();
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
