package com.yunki.lessonpt.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordEncoderTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void matchesEncodedPasswordAndRejectsWrongPassword() {
        String hash = encoder.encode("Abcdef1!");

        assertThat(hash).doesNotContain("Abcdef1!");
        assertThat(encoder.matches("Abcdef1!", hash)).isTrue();
        assertThat(encoder.matches("Wrong123!", hash)).isFalse();
    }
}
