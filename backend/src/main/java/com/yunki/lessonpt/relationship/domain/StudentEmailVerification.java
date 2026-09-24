package com.yunki.lessonpt.relationship.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 학생 이메일 OTP 검증 행이다.
 * 이메일과 OTP 원문은 두지 않고 hash만 보관한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class StudentEmailVerification {

    private Long verificationId;
    private Long teacherStudentAccessId;
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
