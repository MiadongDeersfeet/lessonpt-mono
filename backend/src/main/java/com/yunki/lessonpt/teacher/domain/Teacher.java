package com.yunki.lessonpt.teacher.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 강사 계정이다.
 *
 * 비밀번호 해시는 로그인 검증에만 쓰고, API 응답으로는 나가지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class Teacher {

    private Long teacherId;
    private String email;
    private String passwordHash;
    private String name;
    private String phone;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime deletedAt;
}
