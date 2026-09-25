package com.yunki.lessonpt.relationship.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일반 학생 로그인 OTP다. 초대 링크 OTP와 테이블을 나누어 접근권한 검증과 섞이지 않게 한다.
 * 이메일과 OTP 원문은 두지 않고 hash만 보관한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class StudentLoginVerification {

    private Long verificationId;
    private Long studentId;
    private String emailHash;
    private String codeHash;
    private StudentEmailVerificationStatus verificationStatus;
    private Integer failedAttemptCount;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private LocalDateTime invalidatedAt;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
