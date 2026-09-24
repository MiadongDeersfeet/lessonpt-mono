package com.yunki.lessonpt.relationship.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 강사 화면용 장소 연결 조회 결과다.
 * 관계의 내부 상태와 다른 강사 정보는 담지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class TeacherStudentLocationView {

    private Long teacherStudentLocationId;
    private Long locationId;
    private String locationName;
    private String address;
}
