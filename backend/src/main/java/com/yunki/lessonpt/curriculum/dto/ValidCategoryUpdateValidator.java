package com.yunki.lessonpt.curriculum.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidCategoryUpdateValidator implements ConstraintValidator<ValidCategoryUpdate, CategoryUpdateRequest> {

    @Override
    public boolean isValid(CategoryUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        if (request.isNameSpecified() && (request.getName() == null || request.getName().isBlank())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("이름은 필수입니다.")
                    .addPropertyNode("name")
                    .addConstraintViolation();
            return false;
        }
        if (request.isNameSpecified() && request.getName().length() > 200) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("이름은 200자 이하여야 합니다.")
                    .addPropertyNode("name")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
