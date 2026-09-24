package com.yunki.lessonpt.relationship.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.ProgressStatus;
import com.yunki.lessonpt.common.model.RecordStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 한 수강 배정 안에서 내용 하나를 학습 대상으로 둔 기록이다.
 * 표시 순서는 같은 수강 배정 안에서만 의미를 가진다.
 * 커리큘럼의 모든 내용을 자동으로 만들지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class StudentMonitoring {

    private Long monitoringId;
    private Long studentCurriculumId;
    private Long contentDetailId;
    private Integer displayOrder;
    private Integer currentBpm;
    private ProgressStatus progressStatus;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime deletedAt;
}
