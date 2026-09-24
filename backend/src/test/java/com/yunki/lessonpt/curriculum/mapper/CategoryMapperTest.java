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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.yunki.lessonpt.common.mybatis.typehandler.RecordStatusTypeHandler;

class CategoryMapperTest {

    private static final String NAMESPACE = "com.yunki.lessonpt.curriculum.mapper.CategoryMapper";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() {
        configuration = new Configuration();
        String resource = "mapper/curriculum/CategoryMapper.xml";
        InputStream inputStream = CategoryMapperTest.class.getClassLoader().getResourceAsStream(resource);
        assertThat(inputStream).isNotNull();
        new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
    }

    @Test
    void resultMapMatchesDomainFields() {
        ResultMap resultMap = configuration.getResultMap(NAMESPACE + ".categoryResultMap");
        Set<String> properties = resultMap.getResultMappings().stream()
                .map(ResultMapping::getProperty)
                .collect(Collectors.toSet());
        assertThat(properties).containsExactlyInAnyOrder(
                "categoryId", "curriculumId", "name", "displayOrder", "createdAt", "updatedAt", "status", "deletedAt");
        ResultMapping status = resultMap.getResultMappings().stream()
                .filter(mapping -> "status".equals(mapping.getProperty()))
                .findFirst()
                .orElseThrow();
        assertThat(status.getTypeHandler()).isInstanceOf(RecordStatusTypeHandler.class);
        assertThat(resultMap.getIdResultMappings()).extracting(ResultMapping::getColumn)
                .containsExactly("CATEGORY_ID");
    }

    @Test
    void insertUsesSequenceBeforeInsert() throws Exception {
        MappedStatement insert = configuration.getMappedStatement(NAMESPACE + ".insertCategory");
        MappedStatement selectKey = configuration.getMappedStatement(NAMESPACE + ".insertCategory!selectKey");
        String insertSql = sql(NAMESPACE + ".insertCategory");
        String keySql = selectKey.getSqlSource().getBoundSql(null).getSql();

        assertThat(insert.getKeyGenerator()).isInstanceOf(org.apache.ibatis.executor.keygen.SelectKeyGenerator.class);
        assertThat(keySql).contains("SEQ_CATEGORY.NEXTVAL");
        assertThat(mapperXml()).contains("order=\"BEFORE\"");
        assertThat(insertSql).contains("CATEGORY_ID");
        assertThat(insertSql).doesNotContain("MAX(");
        assertThat(insertSql).doesNotContain("SELECT *");
    }

    @Test
    void activeListOrdersByDisplayOrderInsideCurriculum() {
        String list = sql(NAMESPACE + ".selectActiveCategoriesByCurriculumId");
        String max = sql(NAMESPACE + ".selectMaxDisplayOrderByCurriculumId");
        String active = sql(NAMESPACE + ".selectActiveCategoryById");

        assertThat(active).contains("STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(list).contains("CURRICULUM_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(list).contains("ORDER BY DISPLAY_ORDER ASC, CATEGORY_ID ASC");
        assertThat(max).contains("MAX(DISPLAY_ORDER)", "CURRICULUM_ID = ?");
        assertThat(max).doesNotContain("FOR UPDATE");
        assertThat(list).doesNotContain("SELECT *");
    }

    @Test
    void softDeleteRestoreShiftAndLockStayInsideCurriculum() {
        String softDelete = sql(NAMESPACE + ".softDeleteCategory");
        String restore = sql(NAMESPACE + ".restoreCategory");
        String shift = sql(NAMESPACE + ".shiftActiveDisplayOrdersDown");
        String update = sql(NAMESPACE + ".updateCategory");

        assertThat(softDelete).contains("STATUS = 'N'", "DELETED_AT = SYSTIMESTAMP", "CURRICULUM_ID = ?");
        assertThat(restore).contains("STATUS = 'Y'", "DELETED_AT = NULL", "DISPLAY_ORDER = ?", "CURRICULUM_ID = ?");
        assertThat(shift).contains("DISPLAY_ORDER = DISPLAY_ORDER - 1", "CURRICULUM_ID = ?", "DISPLAY_ORDER > ?");
        assertThat(update).contains("NAME = ?").doesNotContain("DISPLAY_ORDER =");
        assertThat(sql(NAMESPACE + ".lockCategoryById")).contains("FOR UPDATE WAIT 3");
    }

    private static String sql(String statementId) {
        return configuration.getMappedStatement(statementId).getSqlSource().getBoundSql(null).getSql();
    }

    private static String mapperXml() throws java.io.IOException {
        try (InputStream inputStream = CategoryMapperTest.class.getClassLoader()
                .getResourceAsStream("mapper/curriculum/CategoryMapper.xml")) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
