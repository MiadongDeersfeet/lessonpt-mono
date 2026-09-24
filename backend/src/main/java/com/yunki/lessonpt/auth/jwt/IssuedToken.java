package com.yunki.lessonpt.auth.jwt;

import java.time.LocalDateTime;

public record IssuedToken(String value, String jti, LocalDateTime expiresAt) {
}
