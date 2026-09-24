package com.yunki.lessonpt.relationship.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidHomeworkUpdateValidator implements ConstraintValidator<ValidHomeworkUpdate, HomeworkUpdateRequest> {

    @Override
    public boolean isValid(HomeworkUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        if (request.isHomeworkContentSpecified()
                && (request.getHomeworkContent() == null || request.getHomeworkContent().isBlank())) {
            return violation(context, "homeworkContent", "과제 내용은 필수입니다.");
        }
        if (request.isCompletedSpecified() && request.getCompleted() == null) {
            return violation(context, "completed", "완료 여부는 비울 수 없습니다.");
        }
        return true;
    }

    private boolean violation(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
