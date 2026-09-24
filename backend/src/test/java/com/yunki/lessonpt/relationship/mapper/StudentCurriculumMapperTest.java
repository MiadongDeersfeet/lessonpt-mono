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

class StudentCurriculumMapperTest {

    private static final String NAMESPACE = "com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() {
        configuration = new Configuration();
        String resource = "mapper/relationship/StudentCurriculumMapper.xml";
        InputStream inputStream = StudentCurriculumMapperTest.class.getClassLoader().getResourceAsStream(resource);
        assertThat(inputStream).isNotNull();
        new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
    }

    @Test
    void resultMapMatchesDomainFields() {
        ResultMap resultMap = configuration.getResultMap(NAMESPACE + ".studentCurriculumResultMap");
        Set<String> properties = resultMap.getResultMappings().stream()
                .map(ResultMapping::getProperty)
                .collect(Collectors.toSet());
        assertThat(properties).containsExactlyInAnyOrder(
                "studentCurriculumId", "teacherStudentLocationId", "curriculumId", "reenrolled", "memo",
                "createdAt", "updatedAt", "status", "deletedAt");
        ResultMapping reenrolled = mapping(resultMap, "reenrolled");
        assertThat(reenrolled.getTypeHandler()).isInstanceOf(BooleanYnTypeHandler.class);
        assertThat(mapping(resultMap, "memo").getJdbcType()).isEqualTo(JdbcType.CLOB);
        assertThat(mapping(resultMap, "status").getTypeHandler()).isInstanceOf(RecordStatusTypeHandler.class);
        assertThat(resultMap.getIdResultMappings()).extracting(ResultMapping::getColumn)
                .containsExactly("STUDENT_CURRICULUM_ID");
    }

    @Test
    void insertUsesSequenceBeforeInsert() throws Exception {
        MappedStatement insert = configuration.getMappedStatement(NAMESPACE + ".insertStudentCurriculum");
        MappedStatement selectKey = configuration.getMappedStatement(NAMESPACE + ".insertStudentCurriculum!selectKey");
        String insertSql = sql(NAMESPACE + ".insertStudentCurriculum");
        String keySql = selectKey.getSqlSource().getBoundSql(null).getSql();
        String xml = mapperXml();

        assertThat(insert.getKeyGenerator()).isInstanceOf(org.apache.ibatis.executor.keygen.SelectKeyGenerator.class);
        assertThat(keySql).contains("SEQ_STUDENT_CURRICULUM.NEXTVAL");
        assertThat(xml).contains("order=\"BEFORE\"");
        assertThat(insertSql).contains("STUDENT_CURRICULUM_ID", "IS_REENROLLED");
        assertThat(insertSql).doesNotContain("MAX(");
        assertThat(xml).doesNotContain("SELECT *").doesNotContain("${").doesNotContain("DISPLAY_ORDER");
    }

    @Test
    void activeAndPairQueriesStayOnTheLocationCurriculumPair() {
        String active = sql(NAMESPACE + ".selectActiveStudentCurriculumById");
        String pair = sql(NAMESPACE + ".selectByTeacherStudentLocationIdAndCurriculumId");
        String activePair = sql(NAMESPACE + ".selectActiveByTeacherStudentLocationIdAndCurriculumId");
        String list = sql(NAMESPACE + ".selectActiveStudentCurriculumsByTeacherStudentLocationId");

        assertThat(active).contains("STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(pair).contains("TEACHER_STUDENT_LOCATION_ID = ?", "CURRICULUM_ID = ?");
        assertThat(pair).doesNotContain("STATUS = 'Y'");
        assertThat(activePair).contains(
                "TEACHER_STUDENT_LOCATION_ID = ?", "CURRICULUM_ID = ?", "STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(list).contains("TEACHER_STUDENT_LOCATION_ID = ?", "ORDER BY CREATED_AT ASC, STUDENT_CURRICULUM_ID ASC");
        assertThat(list).doesNotContain("SELECT *");
    }

    @Test
    void updateMemoSoftDeleteRestoreAndLock() throws Exception {
        String update = sql(NAMESPACE + ".updateStudentCurriculum");
        String softDelete = sql(NAMESPACE + ".softDeleteStudentCurriculum");
        String restore = sql(NAMESPACE + ".restoreStudentCurriculum");
        String xml = mapperXml();

        assertThat(update).contains("MEMO = ?", "TEACHER_STUDENT_LOCATION_ID = ?");
        assertThat(update).doesNotContain("IS_REENROLLED");
        assertThat(xml).contains("jdbcType=CLOB");
        assertThat(softDelete).contains(
                "STATUS = 'N'", "DELETED_AT = SYSTIMESTAMP", "STUDENT_CURRICULUM_ID = ?", "TEACHER_STUDENT_LOCATION_ID = ?");
        assertThat(restore).contains("STATUS = 'Y'", "DELETED_AT = NULL", "IS_REENROLLED = 'Y'", "DELETED_AT IS NOT NULL");
        assertThat(sql(NAMESPACE + ".lockStudentCurriculumById")).contains("FOR UPDATE WAIT 3");
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
        try (InputStream inputStream = StudentCurriculumMapperTest.class.getClassLoader()
                .getResourceAsStream("mapper/relationship/StudentCurriculumMapper.xml")) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
