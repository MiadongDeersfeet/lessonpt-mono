package com.yunki.lessonpt.curriculum.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PATCH에서 필드를 빼면 setter가 호출되지 않아 기존 값을 유지한다.
 * nullable 필드의 null은 setter가 호출되므로 해당 값을 제거한다.
 */
@ValidContentDetailUpdate
public class ContentDetailUpdateRequest {

    private String name;
    private boolean nameSpecified;
    private String memo;
    private boolean memoSpecified;
    private Integer targetBpm;
    private boolean targetBpmSpecified;
    private String evaluationMemo;
    private boolean evaluationMemoSpecified;
    private String sheetUrl;
    private boolean sheetUrlSpecified;
    private String youtubeUrl;
    private boolean youtubeUrlSpecified;
    private String audioUrl;
    private boolean audioUrlSpecified;

    @JsonIgnore
    public boolean isNameSpecified() {
        return nameSpecified;
    }

    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.nameSpecified = true;
        this.name = name;
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

    @JsonIgnore
    public boolean isTargetBpmSpecified() {
        return targetBpmSpecified;
    }

    public Integer getTargetBpm() {
        return targetBpm;
    }

    @JsonProperty("targetBpm")
    public void setTargetBpm(Integer targetBpm) {
        this.targetBpmSpecified = true;
        this.targetBpm = targetBpm;
    }

    @JsonIgnore
    public boolean isEvaluationMemoSpecified() {
        return evaluationMemoSpecified;
    }

    public String getEvaluationMemo() {
        return evaluationMemo;
    }

    @JsonProperty("evaluationMemo")
    public void setEvaluationMemo(String evaluationMemo) {
        this.evaluationMemoSpecified = true;
        this.evaluationMemo = evaluationMemo;
    }

    @JsonIgnore
    public boolean isSheetUrlSpecified() {
        return sheetUrlSpecified;
    }

    public String getSheetUrl() {
        return sheetUrl;
    }

    @JsonProperty("sheetUrl")
    public void setSheetUrl(String sheetUrl) {
        this.sheetUrlSpecified = true;
        this.sheetUrl = sheetUrl;
    }

    @JsonIgnore
    public boolean isYoutubeUrlSpecified() {
        return youtubeUrlSpecified;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    @JsonProperty("youtubeUrl")
    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrlSpecified = true;
        this.youtubeUrl = youtubeUrl;
    }

    @JsonIgnore
    public boolean isAudioUrlSpecified() {
        return audioUrlSpecified;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    @JsonProperty("audioUrl")
    public void setAudioUrl(String audioUrl) {
        this.audioUrlSpecified = true;
        this.audioUrl = audioUrl;
    }
}
