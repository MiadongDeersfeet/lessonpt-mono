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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.yunki.lessonpt.common.mybatis.typehandler.RecordStatusTypeHandler;

class TeacherStudentAccessMapperTest {

    private static final String NAMESPACE = "com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() {
        configuration = new Configuration();
        String resource = "mapper/relationship/TeacherStudentAccessMapper.xml";
        InputStream inputStream = TeacherStudentAccessMapperTest.class.getClassLoader().getResourceAsStream(resource);
        assertThat(inputStream).isNotNull();
        new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
    }

    @Test
    void resultMapMatchesDomainFields() {
        ResultMap resultMap = configuration.getResultMap(NAMESPACE + ".teacherStudentAccessResultMap");
        Set<String> properties = resultMap.getResultMappings().stream()
                .map(ResultMapping::getProperty)
                .collect(Collectors.toSet());
        assertThat(properties).containsExactlyInAnyOrder(
                "teacherStudentAccessId",
                "teacherStudentId",
                "publicAccessKey",
                "enabledAt",
                "lastVerifiedAt",
                "createdAt",
                "updatedAt",
                "status",
                "revokedAt");
        assertThat(mapping(resultMap, "status").getTypeHandler()).isInstanceOf(RecordStatusTypeHandler.class);
        assertThat(resultMap.getIdResultMappings()).extracting(ResultMapping::getColumn)
                .containsExactly("TEACHER_STUDENT_ACCESS_ID");
    }

    @Test
    void statementsAvoidSelectStarSubstitutionAndPhysicalDelete() throws Exception {
        String xml = mapperXml();
        assertThat(xml).doesNotContain("SELECT *").doesNotContain("${").doesNotContain("<delete");
        assertThat(sql(NAMESPACE + ".selectActiveByTeacherStudentId")).contains("STATUS = 'Y'", "REVOKED_AT IS NULL");
        assertThat(sql(NAMESPACE + ".selectByPublicAccessKey")).contains(
                "PUBLIC_ACCESS_KEY = ?", "STATUS = 'Y'", "REVOKED_AT IS NULL");
        String reactivate = sql(NAMESPACE + ".reactivateTeacherStudentAccess");
        assertThat(reactivate).contains(
                "PUBLIC_ACCESS_KEY = ?", "STATUS = 'Y'", "REVOKED_AT = NULL", "STATUS = 'N'", "REVOKED_AT IS NOT NULL");
        String softDelete = sql(NAMESPACE + ".softDeleteActiveByTeacherStudentId");
        assertThat(softDelete).contains("STATUS = 'N'", "REVOKED_AT = SYSTIMESTAMP");
        assertThat(softDelete).doesNotContain("DELETE FROM");
        MappedStatement insert = configuration.getMappedStatement(NAMESPACE + ".insertTeacherStudentAccess");
        String keySql = configuration.getMappedStatement(NAMESPACE + ".insertTeacherStudentAccess!selectKey")
                .getSqlSource().getBoundSql(null).getSql();
        assertThat(keySql).contains("SEQ_TEACHER_STUDENT_ACCESS.NEXTVAL");
        assertThat(xml).contains("order=\"BEFORE\"");
        assertThat(insert.getSqlSource().getBoundSql(null).getSql()).contains("PUBLIC_ACCESS_KEY");
        assertThat(insert.getSqlSource().getBoundSql(null).getSql()).doesNotContain("useGeneratedKeys");
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
        try (InputStream inputStream = TeacherStudentAccessMapperTest.class.getClassLoader()
                .getResourceAsStream("mapper/relationship/TeacherStudentAccessMapper.xml")) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
