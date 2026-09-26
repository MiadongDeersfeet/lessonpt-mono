package com.yunki.lessonpt.student.query;

import com.yunki.lessonpt.common.model.ProgressStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentPortalMonitoringRow {

    private Long monitoringId;
    private Long studentCurriculumId;
    private Long categoryId;
    private Long contentDetailId;
    private String contentName;
    private Integer targetBpm;
    private String youtubeUrl;
    private Integer currentBpm;
    private ProgressStatus progressStatus;
}
