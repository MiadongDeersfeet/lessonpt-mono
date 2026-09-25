package com.yunki.lessonpt.relationship.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentLearningCurriculumRow {
    private Long teacherStudentLocationId;
    private Long studentCurriculumId;
    private Long curriculumId;
    private String curriculumName;
    private Boolean reenrolled;
    private String memo;
}
