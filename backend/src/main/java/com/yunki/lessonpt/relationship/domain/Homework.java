package com.yunki.lessonpt.relationship.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 한 학습 모니터링에 달린 과제다.
 * 같은 모니터링에 여러 과제가 있을 수 있고, 표시 순서는 없다.
 * 완료 여부는 모니터링 진행 상태와 연결되지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class Homework {

    private Long homeworkId;
    private Long monitoringId;
    private String homeworkContent;
    private LocalDateTime deadline;
    private Boolean completed;
    private String feedback;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime deletedAt;
}
