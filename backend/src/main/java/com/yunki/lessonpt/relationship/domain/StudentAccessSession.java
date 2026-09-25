package com.yunki.lessonpt.relationship.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 학생 조회 세션이다. 토큰 원문은 두지 않고 hash만 보관한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class StudentAccessSession {

    private Long studentAccessSessionId;
    private Long teacherStudentAccessId;
    private String sessionTokenHash;
    private StudentAccessSessionStatus sessionStatus;
    private LocalDateTime expiresAt;
    private LocalDateTime absoluteExpiresAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime revokedAt;
}
