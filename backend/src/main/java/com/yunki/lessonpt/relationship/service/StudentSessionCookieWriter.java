package com.yunki.lessonpt.relationship.service;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.yunki.lessonpt.relationship.config.StudentSessionProperties;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StudentSessionCookieWriter {

    private final StudentSessionProperties properties;

    public void write(HttpServletResponse response, IssuedStudentSession issued) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(issued.rawToken(), Duration.ofDays(180)).toString());
    }

    public void expire(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(StudentSessionProperties.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.isSecure())
                .path(StudentSessionProperties.COOKIE_PATH)
                .maxAge(maxAge);
        String sameSite = properties.getSameSite();
        if (sameSite != null && !sameSite.isBlank()) {
            builder.sameSite(sameSite.trim());
        }
        return builder.build();
    }
}
