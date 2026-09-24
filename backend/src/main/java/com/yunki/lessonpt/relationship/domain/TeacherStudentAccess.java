package com.yunki.lessonpt.relationship.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TeacherStudent 관계의 학생 조회 접근권한이다.
 * PUBLIC_ACCESS_KEY는 관계를 찾는 식별자이며 인증 수단이 아니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class TeacherStudentAccess {

    private Long teacherStudentAccessId;
    private Long teacherStudentId;
    private String publicAccessKey;
    private LocalDateTime enabledAt;
    private LocalDateTime lastVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime revokedAt;
}
