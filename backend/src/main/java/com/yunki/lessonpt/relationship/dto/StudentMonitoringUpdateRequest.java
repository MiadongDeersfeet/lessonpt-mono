package com.yunki.lessonpt.relationship.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yunki.lessonpt.common.model.ProgressStatus;

/**
 * PATCH에서 필드를 빼면 setter가 호출되지 않아 기존 값을 유지한다.
 * currentBpm과 memo의 null은 값을 제거한다.
 * progressStatus의 null은 허용하지 않는다.
 */
@ValidStudentMonitoringUpdate
public class StudentMonitoringUpdateRequest {

    private Integer currentBpm;
    private boolean currentBpmSpecified;
    private ProgressStatus progressStatus;
    private boolean progressStatusSpecified;
    private String memo;
    private boolean memoSpecified;

    @JsonIgnore
    public boolean isCurrentBpmSpecified() {
        return currentBpmSpecified;
    }

    public Integer getCurrentBpm() {
        return currentBpm;
    }

    @JsonProperty("currentBpm")
    public void setCurrentBpm(Integer currentBpm) {
        this.currentBpmSpecified = true;
        this.currentBpm = currentBpm;
    }

    @JsonIgnore
    public boolean isProgressStatusSpecified() {
        return progressStatusSpecified;
    }

    public ProgressStatus getProgressStatus() {
        return progressStatus;
    }

    @JsonProperty("progressStatus")
    public void setProgressStatus(ProgressStatus progressStatus) {
        this.progressStatusSpecified = true;
        this.progressStatus = progressStatus;
    }

    @JsonIgnore
    public boolean isMemoSpecified() {
        return memoSpecified;
    }

    public String getMemo() {
        return memo;
    }

    @JsonProperty("memo")
    public void setMemo(String memo) {
        this.memoSpecified = true;
        this.memo = memo;
    }
}
