package com.yunki.lessonpt.student.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentPortalCategoryRow {

    private Long curriculumId;
    private Long categoryId;
    private String categoryName;
    private Integer totalContentCount;
}
