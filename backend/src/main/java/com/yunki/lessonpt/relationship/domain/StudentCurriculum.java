package com.yunki.lessonpt.relationship.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 강사-학생의 한 수업 장소에서 커리큘럼을 수강하는 배정이다.
 * 같은 장소와 커리큘럼 조합은 하나다. 다른 장소의 같은 커리큘럼은 별도 배정이다.
 */
@Getter
@Setter
@NoArgsConstructor
public class StudentCurriculum {

    private Long studentCurriculumId;
    private Long teacherStudentLocationId;
    private Long curriculumId;
    private Boolean reenrolled;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime deletedAt;
}
