package com.yunki.lessonpt.student.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentPortalRelationshipRow {

    private Long teacherStudentAccessId;
    private Long teacherStudentId;
    private String teacherName;
    private Long locationId;
    private String locationName;
}
