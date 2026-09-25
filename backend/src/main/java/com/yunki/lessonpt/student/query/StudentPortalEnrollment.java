package com.yunki.lessonpt.student.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentPortalEnrollment {

    private Long studentCurriculumId;
    private Long curriculumId;
    private String curriculumName;
    private Integer curriculumDisplayOrder;
}
