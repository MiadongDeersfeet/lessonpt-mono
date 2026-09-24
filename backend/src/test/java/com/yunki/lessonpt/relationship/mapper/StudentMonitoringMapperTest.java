package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.yunki.lessonpt.common.mybatis.typehandler.ProgressStatusTypeHandler;
import com.yunki.lessonpt.common.mybatis.typehandler.RecordStatusTypeHandler;

class StudentMonitoringMapperTest {

    private static final String NAMESPACE = "com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() {
        configuration = new Configuration();
        String resource = "mapper/relationship/StudentMonitoringMapper.xml";
        InputStream inputStream = StudentMonitoringMapperTest.class.getClassLoader().getResourceAsStream(resource);
        assertThat(inputStream).isNotNull();
        new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
    }

    @Test
    void resultMapMatchesDomainFields() {
        ResultMap resultMap = configuration.getResultMap(NAMESPACE + ".studentMonitoringResultMap");
        Set<String> properties = resultMap.getResultMappings().stream()
                .map(ResultMapping::getProperty)
                .collect(Collectors.toSet());
        assertThat(properties).containsExactlyInAnyOrder(
                "monitoringId", "studentCurriculumId", "contentDetailId", "displayOrder",
                "currentBpm", "progressStatus", "memo", "createdAt", "updatedAt", "status", "deletedAt");
        assertThat(mapping(resultMap, "progressStatus").getTypeHandler()).isInstanceOf(ProgressStatusTypeHandler.class);
        assertThat(mapping(resultMap, "memo").getJdbcType()).isEqualTo(JdbcType.CLOB);
        assertThat(mapping(resultMap, "status").getTypeHandler()).isInstanceOf(RecordStatusTypeHandler.class);
        assertThat(resultMap.getIdResultMappings()).extracting(ResultMapping::getColumn)
                .containsExactly("MONITORING_ID");
    }

    @Test
    void insertUsesSequenceBeforeInsert() throws Exception {
        MappedStatement insert = configuration.getMappedStatement(NAMESPACE + ".insertStudentMonitoring");
        MappedStatement selectKey = configuration.getMappedStatement(NAMESPACE + ".insertStudentMonitoring!selectKey");
        String xml = mapperXml();

        String insertSql = sql(NAMESPACE + ".insertStudentMonitoring");
        assertThat(insert.getKeyGenerator()).isInstanceOf(org.apache.ibatis.executor.keygen.SelectKeyGenerator.class);
        assertThat(selectKey.getSqlSource().getBoundSql(null).getSql()).contains("SEQ_MONITORING.NEXTVAL");
        assertThat(xml).contains("keyProperty=\"monitoringId\"").contains("order=\"BEFORE\"");
        assertThat(insertSql).contains("MONITORING_ID");
        assertThat(insertSql).doesNotContain("MAX(").doesNotContain("SELECT *");
        assertThat(xml).doesNotContain("SELECT *").doesNotContain("${");
    }

    @Test
    void activeListAndMaxStayInsideStudentCurriculum() {
        String pair = sql(NAMESPACE + ".selectByStudentCurriculumIdAndContentDetailId");
        String activePair = sql(NAMESPACE + ".selectActiveByStudentCurriculumIdAndContentDetailId");
        String list = sql(NAMESPACE + ".selectActiveStudentMonitoringsByStudentCurriculumId");
        String max = sql(NAMESPACE + ".selectMaxDisplayOrderByStudentCurriculumId");

        assertThat(sql(NAMESPACE + ".selectActiveStudentMonitoringById")).contains("STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(pair).contains("STUDENT_CURRICULUM_ID = ?", "CONTENT_DETAIL_ID = ?");
        assertThat(pair).doesNotContain("STATUS = 'Y'");
        assertThat(activePair).contains("STUDENT_CURRICULUM_ID = ?", "CONTENT_DETAIL_ID = ?", "STATUS = 'Y'");
        assertThat(list).contains("STUDENT_CURRICULUM_ID = ?", "ORDER BY DISPLAY_ORDER ASC, MONITORING_ID ASC");
        assertThat(max).contains("MAX(DISPLAY_ORDER)", "STUDENT_CURRICULUM_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(max).doesNotContain("FOR UPDATE");
    }

    @Test
    void updateLeavesOrderAndRestoreKeepsProgressFields() throws Exception {
        String update = sql(NAMESPACE + ".updateStudentMonitoring");
        String restore = sql(NAMESPACE + ".restoreStudentMonitoring");
        String shift = sql(NAMESPACE + ".shiftActiveDisplayOrdersDown");
        String xml = mapperXml();

        assertThat(update).contains("CURRENT_BPM = ?", "PROGRESS_STATUS = ?", "MEMO = ?", "STUDENT_CURRICULUM_ID = ?");
        assertThat(update).doesNotContain("DISPLAY_ORDER");
        assertThat(xml).contains("jdbcType=NUMERIC", "jdbcType=CLOB");
        assertThat(sql(NAMESPACE + ".softDeleteStudentMonitoring")).contains(
                "STATUS = 'N'", "DELETED_AT = SYSTIMESTAMP", "MONITORING_ID = ?", "STUDENT_CURRICULUM_ID = ?");
        assertThat(restore).contains("STATUS = 'Y'", "DELETED_AT = NULL", "DISPLAY_ORDER = ?", "DELETED_AT IS NOT NULL");
        assertThat(restore).doesNotContain("CURRENT_BPM").doesNotContain("PROGRESS_STATUS").doesNotContain("MEMO");
        assertThat(shift).contains(
                "DISPLAY_ORDER = DISPLAY_ORDER - 1", "STUDENT_CURRICULUM_ID = ?", "DISPLAY_ORDER > ?");
        assertThat(sql(NAMESPACE + ".lockStudentMonitoringById")).contains("FOR UPDATE WAIT 3");
    }

    private static ResultMapping mapping(ResultMap resultMap, String property) {
        return resultMap.getResultMappings().stream()
                .filter(item -> property.equals(item.getProperty()))
                .findFirst()
                .orElseThrow();
    }

    private static String sql(String statementId) {
        return configuration.getMappedStatement(statementId).getSqlSource().getBoundSql(null).getSql();
    }

    private static String mapperXml() throws java.io.IOException {
        try (InputStream inputStream = StudentMonitoringMapperTest.class.getClassLoader()
                .getResourceAsStream("mapper/relationship/StudentMonitoringMapper.xml")) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
