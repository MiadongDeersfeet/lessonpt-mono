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

class CurriculumMapperTest {

    private static final String NAMESPACE = "com.yunki.lessonpt.curriculum.mapper.CurriculumMapper";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() {
        configuration = new Configuration();
        String resource = "mapper/curriculum/CurriculumMapper.xml";
        InputStream inputStream = CurriculumMapperTest.class.getClassLoader().getResourceAsStream(resource);
        assertThat(inputStream).isNotNull();
        new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
    }

    @Test
    void resultMapMatchesDomainFields() {
        ResultMap resultMap = configuration.getResultMap(NAMESPACE + ".curriculumResultMap");
        Set<String> properties = resultMap.getResultMappings().stream()
                .map(ResultMapping::getProperty)
                .collect(Collectors.toSet());
        assertThat(properties).containsExactlyInAnyOrder(
                "curriculumId", "teacherId", "name", "displayOrder", "createdAt", "updatedAt", "status", "deletedAt");
        ResultMapping status = resultMap.getResultMappings().stream()
                .filter(mapping -> "status".equals(mapping.getProperty()))
                .findFirst()
                .orElseThrow();
        assertThat(status.getTypeHandler()).isInstanceOf(RecordStatusTypeHandler.class);
        assertThat(resultMap.getIdResultMappings()).extracting(ResultMapping::getColumn)
                .containsExactly("CURRICULUM_ID");
    }

    @Test
    void insertUsesSequenceBeforeInsert() throws Exception {
        MappedStatement insert = configuration.getMappedStatement(NAMESPACE + ".insertCurriculum");
        MappedStatement selectKey = configuration.getMappedStatement(NAMESPACE + ".insertCurriculum!selectKey");
        String insertSql = sql(NAMESPACE + ".insertCurriculum");
        String keySql = selectKey.getSqlSource().getBoundSql(null).getSql();

        assertThat(insert.getKeyGenerator()).isInstanceOf(org.apache.ibatis.executor.keygen.SelectKeyGenerator.class);
        assertThat(keySql).contains("SEQ_CURRICULUM.NEXTVAL");
        assertThat(mapperXml()).contains("order=\"BEFORE\"");
        assertThat(insertSql).contains("CURRICULUM_ID");
        assertThat(insertSql).doesNotContain("MAX(");
        assertThat(insertSql).doesNotContain("SELECT *");
    }

    @Test
    void activeListOrdersByDisplayOrderAndTeacherScope() {
        String list = sql(NAMESPACE + ".selectActiveCurriculumsByTeacherId");
        String max = sql(NAMESPACE + ".selectMaxDisplayOrderByTeacherId");
        String active = sql(NAMESPACE + ".selectActiveCurriculumById");

        assertThat(active).contains("STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(list).contains("TEACHER_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(list).contains("ORDER BY DISPLAY_ORDER ASC, CURRICULUM_ID ASC");
        assertThat(max).contains("MAX(DISPLAY_ORDER)", "TEACHER_ID = ?");
        assertThat(max).doesNotContain("FOR UPDATE");
        assertThat(list).doesNotContain("SELECT *");
    }

    @Test
    void softDeleteRestoreShiftAndLockStayInsideTeacher() {
        String softDelete = sql(NAMESPACE + ".softDeleteCurriculum");
        String restore = sql(NAMESPACE + ".restoreCurriculum");
        String shift = sql(NAMESPACE + ".shiftActiveDisplayOrdersDown");
        String update = sql(NAMESPACE + ".updateCurriculum");

        assertThat(softDelete).contains("STATUS = 'N'", "DELETED_AT = SYSTIMESTAMP", "TEACHER_ID = ?");
        assertThat(restore).contains("STATUS = 'Y'", "DELETED_AT = NULL", "DISPLAY_ORDER = ?", "TEACHER_ID = ?");
        assertThat(shift).contains("DISPLAY_ORDER = DISPLAY_ORDER - 1", "TEACHER_ID = ?", "DISPLAY_ORDER > ?");
        assertThat(update).contains("NAME = ?").doesNotContain("DISPLAY_ORDER =");
        assertThat(sql(NAMESPACE + ".lockCurriculumById")).contains("FOR UPDATE WAIT 3");
    }

    private static String sql(String statementId) {
        return configuration.getMappedStatement(statementId).getSqlSource().getBoundSql(null).getSql();
    }

    private static String mapperXml() throws java.io.IOException {
        try (InputStream inputStream = CurriculumMapperTest.class.getClassLoader()
                .getResourceAsStream("mapper/curriculum/CurriculumMapper.xml")) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
