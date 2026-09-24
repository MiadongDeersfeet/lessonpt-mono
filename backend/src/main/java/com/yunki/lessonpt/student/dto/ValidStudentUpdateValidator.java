package com.yunki.lessonpt.student.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidStudentUpdateValidator implements ConstraintValidator<ValidStudentUpdate, StudentUpdateRequest> {

    @Override
    public boolean isValid(StudentUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        boolean valid = true;
        if (request.isNameSpecified() && (request.getName() == null || request.getName().isBlank())) {
            valid = violation(context, "name", "이름은 필수입니다.");
        } else if (request.isNameSpecified() && request.getName().length() > 100) {
            valid = violation(context, "name", "이름은 100자 이하여야 합니다.");
        }
        if (request.isEmailSpecified() && request.getEmail() != null && request.getEmail().isBlank()) {
            valid = violation(context, "email", "이메일은 비울 수 없습니다.");
        } else if (request.isEmailSpecified() && request.getEmail() != null && !isEmail(request.getEmail().trim())) {
            valid = violation(context, "email", "이메일 형식이 올바르지 않습니다.");
        } else if (request.isEmailSpecified() && request.getEmail() != null && request.getEmail().trim().length() > 255) {
            valid = violation(context, "email", "이메일은 255자 이하여야 합니다.");
        }
        if (request.isPhoneSpecified() && request.getPhone() != null && request.getPhone().isBlank()) {
            valid = violation(context, "phone", "전화번호는 비울 수 없습니다.");
        } else if (request.isPhoneSpecified() && request.getPhone() != null && request.getPhone().length() > 30) {
            valid = violation(context, "phone", "전화번호는 30자 이하여야 합니다.");
        }
        return valid;
    }

    private boolean isEmail(String value) {
        int at = value.indexOf('@');
        return at > 0 && at == value.lastIndexOf('@') && value.indexOf('.', at) > at + 1 && !value.contains(" ");
    }

    private boolean violation(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
