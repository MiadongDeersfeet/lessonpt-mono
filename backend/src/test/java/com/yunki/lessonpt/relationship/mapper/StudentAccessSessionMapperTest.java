package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StudentAccessSessionMapperTest {

    @Test
    void xmlUsesExplicitResultMapAndActiveRevokeAndSlidingUpdates() throws Exception {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/relationship/StudentAccessSessionMapper.xml"));
        assertThat(xml).contains("id=\"studentAccessSessionResultMap\"");
        assertThat(xml).doesNotContain("SELECT *");
        assertThat(xml).doesNotContain("${");
        assertThat(xml).doesNotContain("DELETE FROM");
        assertThat(xml).contains("SESSION_STATUS = 'ACTIVE'");
        assertThat(xml).contains("SESSION_STATUS = 'REVOKED'");
        assertThat(xml).contains("id=\"updateSlidingWindow\"");
        assertThat(xml).contains("EXPIRES_AT = #{expiresAt}");
        assertThat(xml).contains("LAST_ACCESSED_AT = #{lastAccessedAt}");
        assertThat(xml).contains("SEQ_STUDENT_ACCESS_SESSION.NEXTVAL");
    }
}
