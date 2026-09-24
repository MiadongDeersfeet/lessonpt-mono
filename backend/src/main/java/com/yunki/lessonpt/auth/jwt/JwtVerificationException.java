package com.yunki.lessonpt.auth.jwt;

/**
 * 서명이나 만료가 어긋난 토큰이다.
 * 실패 이유는 호출자에게 나누어 알려 주지 않고, 응답은 인증 실패로만 내려간다.
 */
public class JwtVerificationException extends RuntimeException {

    public JwtVerificationException(String message) {
        super(message);
    }
}
