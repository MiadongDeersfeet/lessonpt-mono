package com.yunki.lessonpt.student.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class StudentCreateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void allowsNullEmail() {
        assertThat(validator.validate(new StudentCreateRequest(null, "김학생", null))).isEmpty();
    }

    @Test
    void allowsNormalEmail() {
        assertThat(validator.validate(new StudentCreateRequest("student@lessonpt.local", "김학생", "01012345678"))).isEmpty();
    }

    @Test
    void rejectsInvalidEmail() {
        assertThat(validator.validate(new StudentCreateRequest("not-an-email", "김학생", null))).isNotEmpty();
    }

    @Test
    void rejectsBlankEmail() {
        assertThat(validator.validate(new StudentCreateRequest("  ", "김학생", null))).isNotEmpty();
    }

    @Test
    void rejectsBlankName() {
        assertThat(validator.validate(new StudentCreateRequest(null, "  ", null))).isNotEmpty();
    }

    @Test
    void rejectsPhoneLongerThanThirty() {
        assertThat(validator.validate(new StudentCreateRequest(null, "김학생", "1".repeat(31)))).isNotEmpty();
    }

    @Test
    void allowsPhoneOfThirty() {
        assertThat(validator.validate(new StudentCreateRequest(null, "김학생", "1".repeat(30)))).isEmpty();
    }
}
