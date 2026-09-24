package com.yunki.lessonpt.auth.dto;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn
) {
}
