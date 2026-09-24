package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProgressQueryMapperTest {

    private static final String NAMESPACE = "com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() throws Exception {
        configuration = new Configuration();
        String resource = "mapper/relationship/ProgressQueryMapper.xml";
        try (InputStream inputStream = ProgressQueryMapperTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(inputStream).isNotNull();
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    @Test
    void countsActiveContentDetailsAndCompletedMonitoringOnly() throws Exception {
        String xml = mapperXml();
        String single = sql(NAMESPACE + ".selectProgressByStudentCurriculumId");
        String where = single.substring(single.lastIndexOf("WHERE"));
        String joins = single.substring(0, single.lastIndexOf("WHERE"));

        assertThat(xml).doesNotContain("SELECT *").doesNotContain("${").doesNotContain("TB_HOMEWORK");
        assertThat(xml).doesNotContain("* 100").doesNotContain("PROGRESS_STATUS = 'COMPLETED'");
        assertThat(single).contains("sc.STUDENT_CURRICULUM_ID", "GROUP BY sc.STUDENT_CURRICULUM_ID");
        assertThat(where).contains("sc.STATUS = 'Y'", "sc.DELETED_AT IS NULL");
        assertThat(where).doesNotContain("m.STATUS").doesNotContain("cd.STATUS");
        assertThat(joins).contains("LEFT JOIN TB_CATEGORY c", "LEFT JOIN TB_CONTENT_DETAIL cd");
        assertThat(joins).contains("cd.STATUS = 'Y'", "cd.DELETED_AT IS NULL");
        assertThat(joins).contains("LEFT JOIN TB_STUDENT_MONITORING m");
        assertThat(joins).doesNotContain("INNER JOIN TB_STUDENT_MONITORING");
        assertThat(joins).contains(
                "m.STUDENT_CURRICULUM_ID = sc.STUDENT_CURRICULUM_ID",
                "m.CONTENT_DETAIL_ID = cd.CONTENT_DETAIL_ID",
                "m.STATUS = 'Y'",
                "m.DELETED_AT IS NULL");
        assertThat(single).contains("m.PROGRESS_STATUS = 'Completed'", "COUNT(cd.CONTENT_DETAIL_ID)");
        assertThat(single).doesNotContain("PROGRESS_STATUS = 'Stopped'").doesNotContain("COUNT(c.");
        ResultMap resultMap = configuration.getResultMap(NAMESPACE + ".studentCurriculumProgressResultMap");
        assertThat(resultMap.getIdResultMappings()).extracting(mapping -> mapping.getColumn())
                .containsExactly("STUDENT_CURRICULUM_ID");
    }

    @Test
    void batchQueryUsesInListWithoutPercentage() throws Exception {
        String xml = mapperXml();
        assertThat(xml).contains(
                "selectProgressByStudentCurriculumIds",
                "collection=\"studentCurriculumIds\"",
                "GROUP BY sc.STUDENT_CURRICULUM_ID",
                "ORDER BY sc.STUDENT_CURRICULUM_ID",
                "AND 1 = 0");
        assertThat(xml).doesNotContain("IN ()");
        assertThat(xml).doesNotContain("COUNT(c.CATEGORY_ID)");
    }

    @Test
    void batchOwnershipRequiresActiveParentsOfTheTeacher() {
        String sql = sql(NAMESPACE + ".selectOwnedActiveStudentCurriculumIds");
        assertThat(sql).contains(
                "JOIN TB_TEACHER_STUDENT_LOCATION tsl",
                "JOIN TB_TEACHER_STUDENT ts",
                "JOIN TB_CURRICULUM cu",
                "ts.TEACHER_ID = ?",
                "cu.TEACHER_ID = ?",
                "sc.STATUS = 'Y'",
                "sc.DELETED_AT IS NULL",
                "tsl.STATUS = 'Y'",
                "ts.STATUS = 'Y'",
                "cu.STATUS = 'Y'",
                "AND 1 = 0");
        assertThat(sql).doesNotContain("TB_HOMEWORK", "TB_STUDENT_MONITORING", "* 100");
    }

    private static String sql(String statementId) {
        return configuration.getMappedStatement(statementId).getSqlSource().getBoundSql(null).getSql();
    }

    private static String mapperXml() throws java.io.IOException {
        try (InputStream inputStream = ProgressQueryMapperTest.class.getClassLoader()
                .getResourceAsStream("mapper/relationship/ProgressQueryMapper.xml")) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
