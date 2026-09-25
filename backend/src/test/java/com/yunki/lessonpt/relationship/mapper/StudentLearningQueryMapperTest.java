package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StudentLearningQueryMapperTest {

    @Test
    void sqlKeepsOnlyActiveOwnedRowsAndBatchesIds() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/relationship/StudentLearningQueryMapper.xml"));

        assertThat(xml).doesNotContain("SELECT *");
        assertThat(xml).doesNotContain("${");
        assertThat(xml).contains("relation.TEACHER_ID = #{teacherId}");
        assertThat(xml).contains("relation.STUDENT_ID = #{studentId}");
        assertThat(xml).contains("location.TEACHER_ID = #{teacherId}");
        assertThat(xml).contains("curriculum.TEACHER_ID = #{teacherId}");
        assertThat(xml).contains("STATUS = 'Y'");
        assertThat(xml).contains("DELETED_AT IS NULL");
        assertThat(xml).contains("AND 1 = 0");
        assertThat(xml).contains("enrollment.MEMO");
        assertThat(xml).contains("monitoring.MEMO");
        assertThat(count(xml, "STATUS = 'Y'")).isGreaterThanOrEqualTo(8);
    }

    private static int count(String xml, String token) {
        int count = 0;
        int index = 0;
        while ((index = xml.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
