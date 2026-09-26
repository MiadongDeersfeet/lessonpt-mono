package com.yunki.lessonpt.student.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StudentPortalMapperTest {

    @Test
    void xmlScopesActiveRowsAndAvoidsEmptyInLists() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/student/StudentPortalMapper.xml"));
        assertThat(xml).contains("id=\"studentPortalIdentityResultMap\"");
        assertThat(xml).contains("id=\"studentPortalEnrollmentResultMap\"");
        assertThat(xml).doesNotContain("SELECT *");
        assertThat(xml).doesNotContain("${");
        assertThat(xml).contains("TEACHER_STUDENT_ACCESS_ID = #{teacherStudentAccessId}");
        assertThat(xml).contains("STATUS = 'Y'");
        assertThat(xml).contains("DELETED_AT IS NULL");
        assertThat(xml).contains("AND 1 = 0");
        assertThat(xml).contains("curriculum.DISPLAY_ORDER");
        assertThat(xml).contains("category.DISPLAY_ORDER");
        assertThat(xml).contains("content.DISPLAY_ORDER");
        assertThat(xml).contains("id=\"selectActiveCategories\"");
        assertThat(xml).contains("COUNT(content.CONTENT_DETAIL_ID) AS TOTAL_CONTENT_COUNT");
        assertThat(xml).contains("id=\"selectActiveMonitoringContents\"");
        assertThat(xml).contains("FROM TB_STUDENT_MONITORING monitoring");
        assertThat(xml).doesNotContain("id=\"selectActiveContents\"");
        assertThat(xml).doesNotContain("id=\"selectActiveMonitoring\"");
        assertThat(xml).doesNotContain("MEMO");
        assertThat(xml).doesNotContain("EVALUATION_MEMO");
    }
}
