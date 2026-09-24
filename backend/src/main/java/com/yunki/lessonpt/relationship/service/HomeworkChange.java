package com.yunki.lessonpt.relationship.service;

import java.time.LocalDateTime;

/**
 * 과제 수정 입력이다.
 * setter가 호출된 필드만 변경한다.
 * deadline과 feedback의 명시적 null은 값을 비운다.
 * homeworkContent와 completed의 명시적 null은 허용하지 않는다.
 */
public class HomeworkChange {

    private String homeworkContent;
    private boolean homeworkContentSpecified;
    private LocalDateTime deadline;
    private boolean deadlineSpecified;
    private Boolean completed;
    private boolean completedSpecified;
    private String feedback;
    private boolean feedbackSpecified;

    public boolean isHomeworkContentSpecified() {
        return homeworkContentSpecified;
    }

    public String getHomeworkContent() {
        return homeworkContent;
    }

    public void setHomeworkContent(String homeworkContent) {
        this.homeworkContentSpecified = true;
        this.homeworkContent = homeworkContent;
    }

    public boolean isDeadlineSpecified() {
        return deadlineSpecified;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadlineSpecified = true;
        this.deadline = deadline;
    }

    public boolean isCompletedSpecified() {
        return completedSpecified;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completedSpecified = true;
        this.completed = completed;
    }

    public boolean isFeedbackSpecified() {
        return feedbackSpecified;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedbackSpecified = true;
        this.feedback = feedback;
    }
}
