package com.yunki.lessonpt.student.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentPortalContentRow {

    private Long curriculumId;
    private Long categoryId;
    private String categoryName;
    private Integer categoryDisplayOrder;
    private Long contentDetailId;
    private String contentName;
    private Integer contentDisplayOrder;
    private Integer targetBpm;
    private String youtubeUrl;
}
