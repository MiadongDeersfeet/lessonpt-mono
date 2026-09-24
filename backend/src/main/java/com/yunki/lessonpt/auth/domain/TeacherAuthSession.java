package com.yunki.lessonpt.auth.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 발급된 Access jti와 Refresh 해시만 남긴다.
 * 토큰 원문은 세션 테이블에 두지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class TeacherAuthSession {

    private Long authSessionId;
    private Long teacherId;
    private String accessJti;
    private String refreshTokenHash;
    private LocalDateTime accessExpiresAt;
    private LocalDateTime refreshExpiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
