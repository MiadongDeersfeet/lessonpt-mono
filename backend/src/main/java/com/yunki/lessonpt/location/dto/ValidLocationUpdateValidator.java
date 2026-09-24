package com.yunki.lessonpt.location.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidLocationUpdateValidator implements ConstraintValidator<ValidLocationUpdate, LocationUpdateRequest> {

    @Override
    public boolean isValid(LocationUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        boolean valid = true;
        if (request.isNameSpecified() && (request.getName() == null || request.getName().isBlank())) {
            valid = violation(context, "name", "이름은 필수입니다.");
        } else if (request.isNameSpecified() && request.getName().length() > 150) {
            valid = violation(context, "name", "이름은 150자 이하여야 합니다.");
        }
        if (request.isAddressSpecified() && request.getAddress() != null && request.getAddress().isBlank()) {
            valid = violation(context, "address", "주소는 비울 수 없습니다.");
        } else if (request.isAddressSpecified() && request.getAddress() != null && request.getAddress().length() > 500) {
            valid = violation(context, "address", "주소는 500자 이하여야 합니다.");
        }
        return valid;
    }

    private boolean violation(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
