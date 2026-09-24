package com.yunki.lessonpt.relationship.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 강사와 학습자 프로필의 관계다.
 * 관계 해제는 이 행만 비활성화하고 Student 프로필은 지우지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class TeacherStudent {

    private Long teacherStudentId;
    private Long teacherId;
    private Long studentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime deletedAt;
}
