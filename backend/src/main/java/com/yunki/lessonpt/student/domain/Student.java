package com.yunki.lessonpt.student.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 학습자 프로필이다. 로그인 계정이 아니므로 비밀번호와 역할은 없다.
 * 어느 강사의 학생인지는 이 객체가 아니라 TeacherStudent 관계가 결정한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class Student {

    private Long studentId;
    private String email;
    private String name;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime deletedAt;
}
