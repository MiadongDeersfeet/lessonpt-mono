package com.yunki.lessonpt.relationship.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PATCH에서 memo를 빼면 setter가 호출되지 않아 기존 메모를 유지한다.
 * null은 setter가 호출되므로 메모를 비운다.
 */
public class StudentCurriculumUpdateRequest {

    private String memo;
    private boolean memoSpecified;

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
