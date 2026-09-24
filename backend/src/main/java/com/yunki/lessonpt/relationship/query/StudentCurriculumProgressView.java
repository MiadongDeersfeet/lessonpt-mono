package com.yunki.lessonpt.relationship.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 수강 배정 하나의 진행 개수다.
 * 분모는 커리큘럼의 활성 내용 수이고, 분자는 그중 완료된 활성 모니터링 수다.
 * 백분율은 계산하지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class StudentCurriculumProgressView {

    private Long studentCurriculumId;
    private Integer completedCount;
    private Integer totalCount;
}
