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

import com.yunki.lessonpt.common.mybatis.typehandler.BooleanYnTypeHandler;
import com.yunki.lessonpt.common.mybatis.typehandler.RecordStatusTypeHandler;

class HomeworkMapperTest {

    private static final String NAMESPACE = "com.yunki.lessonpt.relationship.mapper.HomeworkMapper";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() {
        configuration = new Configuration();
        String resource = "mapper/relationship/HomeworkMapper.xml";
        InputStream inputStream = HomeworkMapperTest.class.getClassLoader().getResourceAsStream(resource);
        assertThat(inputStream).isNotNull();
        new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
    }

    @Test
    void resultMapMatchesDomainFields() {
        ResultMap resultMap = configuration.getResultMap(NAMESPACE + ".homeworkResultMap");
        Set<String> properties = resultMap.getResultMappings().stream()
                .map(ResultMapping::getProperty)
                .collect(Collectors.toSet());
        assertThat(properties).containsExactlyInAnyOrder(
                "homeworkId", "monitoringId", "homeworkContent", "deadline", "completed",
                "feedback", "createdAt", "updatedAt", "status", "deletedAt");
        assertThat(mapping(resultMap, "homeworkContent").getJdbcType()).isEqualTo(JdbcType.CLOB);
        assertThat(mapping(resultMap, "feedback").getJdbcType()).isEqualTo(JdbcType.CLOB);
        assertThat(mapping(resultMap, "completed").getTypeHandler()).isInstanceOf(BooleanYnTypeHandler.class);
        assertThat(mapping(resultMap, "status").getTypeHandler()).isInstanceOf(RecordStatusTypeHandler.class);
        assertThat(resultMap.getIdResultMappings()).extracting(ResultMapping::getColumn)
                .containsExactly("HOMEWORK_ID");
    }

    @Test
    void insertUsesSequenceBeforeInsert() throws Exception {
        MappedStatement insert = configuration.getMappedStatement(NAMESPACE + ".insertHomework");
        MappedStatement selectKey = configuration.getMappedStatement(NAMESPACE + ".insertHomework!selectKey");
        String xml = mapperXml();
        String insertSql = sql(NAMESPACE + ".insertHomework");

        assertThat(insert.getKeyGenerator()).isInstanceOf(org.apache.ibatis.executor.keygen.SelectKeyGenerator.class);
        assertThat(selectKey.getSqlSource().getBoundSql(null).getSql()).contains("SEQ_HOMEWORK.NEXTVAL");
        assertThat(xml).contains("keyProperty=\"homeworkId\"").contains("order=\"BEFORE\"");
        assertThat(insertSql).contains("HOMEWORK_ID");
        assertThat(insertSql).doesNotContain("MAX(").doesNotContain("SELECT *");
        assertThat(xml).doesNotContain("SELECT *").doesNotContain("${").doesNotContain("DISPLAY_ORDER");
    }

    @Test
    void activeListStaysInsideMonitoring() {
        String detail = sql(NAMESPACE + ".selectActiveHomeworkByIdAndMonitoringId");
        String list = sql(NAMESPACE + ".selectActiveHomeworksByMonitoringId");

        assertThat(sql(NAMESPACE + ".selectActiveHomeworkById")).contains("STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(detail).contains("HOMEWORK_ID = ?", "MONITORING_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(list).contains("MONITORING_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(list).contains("ORDER BY CREATED_AT ASC, HOMEWORK_ID ASC");
        assertThat(list).doesNotContain("SELECT *");
        String count = sql(NAMESPACE + ".countActiveHomeworksByMonitoringId");
        assertThat(count).contains("COUNT(*)", "MONITORING_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(count).doesNotContain("SELECT *");
    }

    @Test
    void updateLeavesParentAndRestoreKeepsHomeworkFields() throws Exception {
        String update = sql(NAMESPACE + ".updateHomework");
        String restore = sql(NAMESPACE + ".restoreHomework");
        String bulk = sql(NAMESPACE + ".softDeleteActiveHomeworksByMonitoringId");
        String xml = mapperXml();
        String setClause = update.substring(0, update.indexOf("WHERE"));

        assertThat(update).contains("HOMEWORK_CONTENT = ?", "DEADLINE = ?", "IS_COMPLETED = ?", "FEEDBACK = ?");
        assertThat(setClause).doesNotContain("MONITORING_ID");
        assertThat(xml).contains("jdbcType=TIMESTAMP", "jdbcType=CLOB");
        assertThat(sql(NAMESPACE + ".softDeleteHomework")).contains(
                "STATUS = 'N'", "DELETED_AT = SYSTIMESTAMP", "HOMEWORK_ID = ?", "MONITORING_ID = ?");
        assertThat(restore).contains("STATUS = 'Y'", "DELETED_AT = NULL", "DELETED_AT IS NOT NULL");
        assertThat(restore).doesNotContain("HOMEWORK_CONTENT").doesNotContain("DEADLINE")
                .doesNotContain("IS_COMPLETED").doesNotContain("FEEDBACK");
        assertThat(bulk).contains("MONITORING_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(bulk).doesNotContain("HOMEWORK_ID");
        assertThat(sql(NAMESPACE + ".lockHomeworkById")).contains("FOR UPDATE WAIT 3");
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
        try (InputStream inputStream = HomeworkMapperTest.class.getClassLoader()
                .getResourceAsStream("mapper/relationship/HomeworkMapper.xml")) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
