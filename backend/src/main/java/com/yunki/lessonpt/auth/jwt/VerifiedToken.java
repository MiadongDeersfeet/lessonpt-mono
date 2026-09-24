package com.yunki.lessonpt.auth.jwt;

public record VerifiedToken(Long teacherId, String jti) {
}
