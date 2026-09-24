package com.yunki.lessonpt.relationship.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 강사-학생 관계에 연습 장소를 연결한다.
 * 소유권 비교와 중복 관계 처리는 이 Mapper 밖에서 한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class TeacherStudentLocation {

    private Long teacherStudentLocationId;
    private Long teacherStudentId;
    private Long locationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime deletedAt;
}
