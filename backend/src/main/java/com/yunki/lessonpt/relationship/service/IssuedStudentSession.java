package com.yunki.lessonpt.relationship.service;

import java.time.LocalDateTime;

public record IssuedStudentSession(String rawToken, LocalDateTime expiresAt, LocalDateTime absoluteExpiresAt) {
}
