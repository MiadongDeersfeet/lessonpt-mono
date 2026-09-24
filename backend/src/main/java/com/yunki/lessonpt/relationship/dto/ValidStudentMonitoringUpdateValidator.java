package com.yunki.lessonpt.relationship.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidStudentMonitoringUpdateValidator
        implements ConstraintValidator<ValidStudentMonitoringUpdate, StudentMonitoringUpdateRequest> {

    @Override
    public boolean isValid(StudentMonitoringUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        if (request.isCurrentBpmSpecified() && request.getCurrentBpm() != null
                && (request.getCurrentBpm() < 60 || request.getCurrentBpm() > 240)) {
            return violation(context, "currentBpm", "현재 BPM은 60 이상 240 이하여야 합니다.");
        }
        if (request.isProgressStatusSpecified() && request.getProgressStatus() == null) {
            return violation(context, "progressStatus", "진행 상태는 비울 수 없습니다.");
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
