package com.yunki.lessonpt.relationship.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PATCH에서 필드를 빼면 setter가 호출되지 않아 기존 값을 유지한다.
 * deadline과 feedback의 null은 값을 제거한다.
 * homeworkContent와 completed의 null은 허용하지 않는다.
 */
@ValidHomeworkUpdate
public class HomeworkUpdateRequest {

    private String homeworkContent;
    private boolean homeworkContentSpecified;
    private LocalDateTime deadline;
    private boolean deadlineSpecified;
    private Boolean completed;
    private boolean completedSpecified;
    private String feedback;
    private boolean feedbackSpecified;

    @JsonIgnore
    public boolean isHomeworkContentSpecified() {
        return homeworkContentSpecified;
    }

    public String getHomeworkContent() {
        return homeworkContent;
    }

    @JsonProperty("homeworkContent")
    public void setHomeworkContent(String homeworkContent) {
        this.homeworkContentSpecified = true;
        this.homeworkContent = homeworkContent;
    }

    @JsonIgnore
    public boolean isDeadlineSpecified() {
        return deadlineSpecified;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    @JsonProperty("deadline")
    public void setDeadline(LocalDateTime deadline) {
        this.deadlineSpecified = true;
        this.deadline = deadline;
    }

    @JsonIgnore
    public boolean isCompletedSpecified() {
        return completedSpecified;
    }

    public Boolean getCompleted() {
        return completed;
    }

    @JsonProperty("completed")
    public void setCompleted(Boolean completed) {
        this.completedSpecified = true;
        this.completed = completed;
    }

    @JsonIgnore
    public boolean isFeedbackSpecified() {
        return feedbackSpecified;
    }

    public String getFeedback() {
        return feedback;
    }

    @JsonProperty("feedback")
    public void setFeedback(String feedback) {
        this.feedbackSpecified = true;
        this.feedback = feedback;
    }
}
