package com.yunki.lessonpt.curriculum.mapper;

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

import com.yunki.lessonpt.common.mybatis.typehandler.RecordStatusTypeHandler;

class ContentDetailMapperTest {

    private static final String NAMESPACE = "com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() {
        configuration = new Configuration();
        String resource = "mapper/curriculum/ContentDetailMapper.xml";
        InputStream inputStream = ContentDetailMapperTest.class.getClassLoader().getResourceAsStream(resource);
        assertThat(inputStream).isNotNull();
        new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
    }

    @Test
    void resultMapMatchesDomainFieldsIncludingClobs() {
        ResultMap resultMap = configuration.getResultMap(NAMESPACE + ".contentDetailResultMap");
        Set<String> properties = resultMap.getResultMappings().stream()
                .map(ResultMapping::getProperty)
                .collect(Collectors.toSet());
        assertThat(properties).containsExactlyInAnyOrder(
                "contentDetailId", "categoryId", "name", "displayOrder",
                "memo", "targetBpm", "evaluationMemo", "youtubeUrl",
                "createdAt", "updatedAt", "status", "deletedAt");
        ResultMapping status = mapping(resultMap, "status");
        assertThat(status.getTypeHandler()).isInstanceOf(RecordStatusTypeHandler.class);
        assertThat(mapping(resultMap, "memo").getJdbcType()).isEqualTo(JdbcType.CLOB);
        assertThat(mapping(resultMap, "evaluationMemo").getJdbcType()).isEqualTo(JdbcType.CLOB);
        assertThat(resultMap.getIdResultMappings()).extracting(ResultMapping::getColumn)
                .containsExactly("CONTENT_DETAIL_ID");
    }

    @Test
    void insertUsesSequenceBeforeInsert() throws Exception {
        MappedStatement insert = configuration.getMappedStatement(NAMESPACE + ".insertContentDetail");
        MappedStatement selectKey = configuration.getMappedStatement(NAMESPACE + ".insertContentDetail!selectKey");
        String insertSql = sql(NAMESPACE + ".insertContentDetail");
        String keySql = selectKey.getSqlSource().getBoundSql(null).getSql();

        assertThat(insert.getKeyGenerator()).isInstanceOf(org.apache.ibatis.executor.keygen.SelectKeyGenerator.class);
        assertThat(keySql).contains("SEQ_CONTENT_DETAIL.NEXTVAL");
        assertThat(mapperXml()).contains("keyProperty=\"contentDetailId\"").contains("order=\"BEFORE\"");
        assertThat(insertSql).contains("CONTENT_DETAIL_ID");
        assertThat(insertSql).doesNotContain("MAX(");
        assertThat(insertSql).doesNotContain("SELECT *");
    }

    @Test
    void activeListOrdersByDisplayOrderInsideCategory() {
        String list = sql(NAMESPACE + ".selectActiveContentDetailsByCategoryId");
        String max = sql(NAMESPACE + ".selectMaxDisplayOrderByCategoryId");
        String active = sql(NAMESPACE + ".selectActiveContentDetailById");
        String scoped = sql(NAMESPACE + ".selectActiveContentDetailByIdAndCategoryId");

        assertThat(active).contains("STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(scoped).contains("CONTENT_DETAIL_ID = ?", "CATEGORY_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(list).contains("CATEGORY_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(list).contains("ORDER BY DISPLAY_ORDER ASC, CONTENT_DETAIL_ID ASC");
        assertThat(max).contains("MAX(DISPLAY_ORDER)", "CATEGORY_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(max).doesNotContain("FOR UPDATE");
        assertThat(list).doesNotContain("SELECT *");
        assertThat(active).doesNotContain("SELECT *");
    }

    @Test
    void updateWritesNullableFieldsAndLeavesDisplayOrder() throws Exception {
        String update = sql(NAMESPACE + ".updateContentDetail");
        String xml = mapperXml();

        assertThat(update).contains(
                "NAME = ?",
                "MEMO = ?",
                "TARGET_BPM = ?",
                "EVALUATION_MEMO = ?",
                "YOUTUBE_URL = ?",
                "CATEGORY_ID = ?");
        assertThat(update).doesNotContain("DISPLAY_ORDER");
        assertThat(xml).contains("jdbcType=CLOB", "jdbcType=NUMERIC", "jdbcType=VARCHAR");
        assertThat(xml).doesNotContain("<if ");
    }

    @Test
    void softDeleteRestoreShiftAndLockStayInsideCategory() {
        String softDelete = sql(NAMESPACE + ".softDeleteContentDetail");
        String restore = sql(NAMESPACE + ".restoreContentDetail");
        String shift = sql(NAMESPACE + ".shiftActiveDisplayOrdersDown");

        assertThat(softDelete).contains(
                "STATUS = 'N'", "DELETED_AT = SYSTIMESTAMP", "CONTENT_DETAIL_ID = ?", "CATEGORY_ID = ?");
        String bulk = sql(NAMESPACE + ".softDeleteActiveContentDetailsByCategoryId");
        assertThat(bulk).contains(
                "STATUS = 'N'", "DELETED_AT = SYSTIMESTAMP", "CATEGORY_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(bulk).doesNotContain("CONTENT_DETAIL_ID =");
        assertThat(restore).contains(
                "STATUS = 'Y'", "DELETED_AT = NULL", "DISPLAY_ORDER = ?", "CATEGORY_ID = ?");
        assertThat(shift).contains(
                "DISPLAY_ORDER = DISPLAY_ORDER - 1", "CATEGORY_ID = ?", "DISPLAY_ORDER > ?");
        assertThat(sql(NAMESPACE + ".lockContentDetailById")).contains("FOR UPDATE WAIT 3");
        assertThat(softDelete).doesNotContain("SELECT *");
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
        try (InputStream inputStream = ContentDetailMapperTest.class.getClassLoader()
                .getResourceAsStream("mapper/curriculum/ContentDetailMapper.xml")) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
