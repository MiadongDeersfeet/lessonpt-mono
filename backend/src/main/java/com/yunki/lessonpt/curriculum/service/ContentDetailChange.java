package com.yunki.lessonpt.curriculum.service;

/**
 * 생성과 수정에 쓰는 서비스 입력이다.
 * setter가 호출된 필드만 변경 대상으로 본다.
 * 명시적 null은 해당 컬럼을 비운다.
 */
public class ContentDetailChange {

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

    public boolean isNameSpecified() {
        return nameSpecified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.nameSpecified = true;
        this.name = name;
    }

    public boolean isMemoSpecified() {
        return memoSpecified;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memoSpecified = true;
        this.memo = memo;
    }

    public boolean isTargetBpmSpecified() {
        return targetBpmSpecified;
    }

    public Integer getTargetBpm() {
        return targetBpm;
    }

    public void setTargetBpm(Integer targetBpm) {
        this.targetBpmSpecified = true;
        this.targetBpm = targetBpm;
    }

    public boolean isEvaluationMemoSpecified() {
        return evaluationMemoSpecified;
    }

    public String getEvaluationMemo() {
        return evaluationMemo;
    }

    public void setEvaluationMemo(String evaluationMemo) {
        this.evaluationMemoSpecified = true;
        this.evaluationMemo = evaluationMemo;
    }

    public boolean isSheetUrlSpecified() {
        return sheetUrlSpecified;
    }

    public String getSheetUrl() {
        return sheetUrl;
    }

    public void setSheetUrl(String sheetUrl) {
        this.sheetUrlSpecified = true;
        this.sheetUrl = sheetUrl;
    }

    public boolean isYoutubeUrlSpecified() {
        return youtubeUrlSpecified;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrlSpecified = true;
        this.youtubeUrl = youtubeUrl;
    }

    public boolean isAudioUrlSpecified() {
        return audioUrlSpecified;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrlSpecified = true;
        this.audioUrl = audioUrl;
    }
}
