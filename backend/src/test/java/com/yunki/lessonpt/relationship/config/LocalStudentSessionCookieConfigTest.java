package com.yunki.lessonpt.relationship.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LocalStudentSessionCookieConfigTest {

    @Test
    void localProfileRelaxesSecureWithoutChangingTheDefault() throws Exception {
        String application = Files.readString(Path.of("src/main/resources/application.yml"));
        String[] documents = application.split("\\R---\\R", 2);

        assertThat(documents).hasSize(2);
        assertThat(documents[0]).contains("secure: true");
        assertThat(documents[0]).doesNotContain("on-profile: local");
        assertThat(documents[1]).contains("on-profile: local");
        assertThat(documents[1]).contains("secure: false");
        assertThat(documents[1]).contains("same-site: Lax");
        assertThat(documents[0].toLowerCase()).doesNotContain("domain:");
        assertThat(documents[1].toLowerCase()).doesNotContain("domain:");
        assertThat(application.toLowerCase()).doesNotContain("cors");

        String example = Files.readString(Path.of("src/main/resources/application-local.yml.example"));
        assertThat(example).contains("secure: false");
        assertThat(example).contains("same-site: Lax");
        assertThat(example).contains("${LESSONPT_DB_PASSWORD}");
        assertThat(example.toLowerCase()).doesNotContain("domain:");
    }
}
