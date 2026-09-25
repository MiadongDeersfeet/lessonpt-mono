package com.yunki.lessonpt.relationship.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentLearningLocationRow {
    private Long teacherStudentLocationId;
    private Long locationId;
    private String locationName;
    private String address;
}
