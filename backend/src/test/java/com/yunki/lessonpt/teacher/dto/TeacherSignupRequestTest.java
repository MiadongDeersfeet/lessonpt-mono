package com.yunki.lessonpt.teacher.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class TeacherSignupRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidEmail() {
        assertThat(validator.validate(request("not-an-email", "Abcdef1!", "김강사", null))).isNotEmpty();
    }

    @Test
    void rejectsPasswordShorterThanEight() {
        assertThat(validator.validate(request("teacher@lessonpt.local", "Abcde1!", "김강사", null))).isNotEmpty();
    }

    @Test
    void rejectsPasswordLongerThanTwenty() {
        assertThat(validator.validate(request("teacher@lessonpt.local", "Abcdefghij1234567890!", "김강사", null))).isNotEmpty();
    }

    @Test
    void rejectsPasswordWithoutLetter() {
        assertThat(validator.validate(request("teacher@lessonpt.local", "1234567!", "김강사", null))).isNotEmpty();
    }

    @Test
    void rejectsPasswordWithoutDigit() {
        assertThat(validator.validate(request("teacher@lessonpt.local", "Abcdefg!", "김강사", null))).isNotEmpty();
    }

    @Test
    void rejectsPasswordWithoutSpecialCharacter() {
        assertThat(validator.validate(request("teacher@lessonpt.local", "Abcdefg1", "김강사", null))).isNotEmpty();
    }

    @Test
    void rejectsPasswordContainingSpace() {
        assertThat(validator.validate(request("teacher@lessonpt.local", "Abcd ef1!", "김강사", null))).isNotEmpty();
    }

    @Test
    void acceptsPasswordWithLetterDigitAndSpecialCharacter() {
        assertThat(validator.validate(request("teacher@lessonpt.local", "Abcdef1!", "김강사", null))).isEmpty();
    }

    @Test
    void rejectsBlankEmail() {
        assertThat(validator.validate(request(" ", "Abcdef1!", "김강사", null))).isNotEmpty();
        assertThat(validator.validate(request(null, "Abcdef1!", "김강사", null))).isNotEmpty();
    }

    @Test
    void rejectsBlankName() {
        assertThat(validator.validate(request("teacher@lessonpt.local", "Abcdef1!", " ", null))).isNotEmpty();
        assertThat(validator.validate(request("teacher@lessonpt.local", "Abcdef1!", null, null))).isNotEmpty();
    }

    @Test
    void rejectsPhoneLongerThanThirty() {
        assertThat(validator.validate(request("teacher@lessonpt.local", "Abcdef1!", "김강사", "1".repeat(31)))).isNotEmpty();
    }

    @Test
    void acceptsNullPhone() {
        assertThat(validator.validate(request("teacher@lessonpt.local", "Abcdef1!", "김강사", null))).isEmpty();
    }

    private TeacherSignupRequest request(String email, String password, String name, String phone) {
        return new TeacherSignupRequest(email, password, name, phone);
    }
}
