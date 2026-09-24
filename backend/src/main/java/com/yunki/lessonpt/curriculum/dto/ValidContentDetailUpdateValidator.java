package com.yunki.lessonpt.curriculum.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidContentDetailUpdateValidator
        implements ConstraintValidator<ValidContentDetailUpdate, ContentDetailUpdateRequest> {

    @Override
    public boolean isValid(ContentDetailUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        if (request.isNameSpecified() && (request.getName() == null || request.getName().isBlank())) {
            return violation(context, "name", "이름은 필수입니다.");
        }
        if (request.isNameSpecified() && request.getName().length() > 200) {
            return violation(context, "name", "이름은 200자 이하여야 합니다.");
        }
        if (request.isTargetBpmSpecified() && request.getTargetBpm() != null
                && (request.getTargetBpm() < 60 || request.getTargetBpm() > 240)) {
            return violation(context, "targetBpm", "목표 BPM은 60 이상 240 이하여야 합니다.");
        }
        if (request.isSheetUrlSpecified() && request.getSheetUrl() != null && request.getSheetUrl().length() > 2000) {
            return violation(context, "sheetUrl", "악보 URL은 2000자 이하여야 합니다.");
        }
        if (request.isYoutubeUrlSpecified() && request.getYoutubeUrl() != null && request.getYoutubeUrl().length() > 2000) {
            return violation(context, "youtubeUrl", "영상 URL은 2000자 이하여야 합니다.");
        }
        if (request.isAudioUrlSpecified() && request.getAudioUrl() != null && request.getAudioUrl().length() > 2000) {
            return violation(context, "audioUrl", "음원 URL은 2000자 이하여야 합니다.");
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
