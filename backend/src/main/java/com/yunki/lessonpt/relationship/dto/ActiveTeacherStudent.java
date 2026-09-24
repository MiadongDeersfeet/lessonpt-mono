package com.yunki.lessonpt.relationship.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 현재 강사 화면용 조회 결과다. 다른 강사 정보와 삭제 시각은 담지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ActiveTeacherStudent {

    private Long teacherStudentId;
    private Long studentId;
    private String email;
    private String name;
    private String phone;
}
