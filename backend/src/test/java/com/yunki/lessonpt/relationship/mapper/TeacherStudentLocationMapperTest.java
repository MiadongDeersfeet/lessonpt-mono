package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
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

class TeacherStudentLocationMapperTest {

    private static final String NAMESPACE = "com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() {
        configuration = new Configuration();
        String resource = "mapper/relationship/TeacherStudentLocationMapper.xml";
        InputStream inputStream = TeacherStudentLocationMapperTest.class.getClassLoader().getResourceAsStream(resource);
        assertThat(inputStream).isNotNull();
        XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
        builder.parse();
    }

    @Test
    void resultMapMatchesDomainFields() {
        ResultMap resultMap = configuration.getResultMap(NAMESPACE + ".teacherStudentLocationResultMap");
        Set<String> properties = resultMap.getResultMappings().stream()
                .map(ResultMapping::getProperty)
                .collect(Collectors.toSet());

        assertThat(properties).containsExactlyInAnyOrder(
                "teacherStudentLocationId",
                "teacherStudentId",
                "locationId",
                "createdAt",
                "updatedAt",
                "status",
                "deletedAt");
        ResultMapping status = resultMap.getResultMappings().stream()
                .filter(mapping -> "status".equals(mapping.getProperty()))
                .findFirst()
                .orElseThrow();
        assertThat(status.getTypeHandler()).isInstanceOf(RecordStatusTypeHandler.class);
        assertThat(resultMap.getIdResultMappings()).extracting(ResultMapping::getColumn)
                .containsExactly("TEACHER_STUDENT_LOCATION_ID");
    }

    @Test
    void insertUsesSequenceBeforeInsert() throws Exception {
        MappedStatement insert = configuration.getMappedStatement(NAMESPACE + ".insertTeacherStudentLocation");
        MappedStatement selectKey = configuration.getMappedStatement(NAMESPACE + ".insertTeacherStudentLocation!selectKey");
        String insertSql = insert.getSqlSource().getBoundSql(null).getSql();
        String keySql = selectKey.getSqlSource().getBoundSql(null).getSql();

        assertThat(insert.getKeyGenerator()).isInstanceOf(org.apache.ibatis.executor.keygen.SelectKeyGenerator.class);
        assertThat(keySql).contains("SEQ_TEACHER_STUDENT_LOCATION.NEXTVAL");
        String xml = mapperXml();
        assertThat(xml).contains("keyProperty=\"teacherStudentLocationId\"");
        assertThat(xml).contains("order=\"BEFORE\"");
        assertThat(insertSql).contains("TEACHER_STUDENT_LOCATION_ID");
        assertThat(insertSql).doesNotContain("MAX(");
        assertThat(selectKey.getId()).contains("selectKey");
    }

    @Test
    void softDeleteAndRestoreSetStatusAndDeletedAt() {
        String softDelete = sql(NAMESPACE + ".softDeleteTeacherStudentLocation");
        String restore = sql(NAMESPACE + ".restoreTeacherStudentLocation");

        assertThat(softDelete).contains("STATUS = 'N'").contains("DELETED_AT = SYSTIMESTAMP");
        assertThat(restore).contains("STATUS = 'Y'").contains("DELETED_AT = NULL");
    }

    @Test
    void activeQueriesRequireActiveStatusAndLockWaits() {
        assertThat(sql(NAMESPACE + ".selectActiveTeacherStudentLocationById")).contains("STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(sql(NAMESPACE + ".selectActiveByTeacherStudentId")).contains("STATUS = 'Y'", "DELETED_AT IS NULL");
        assertThat(sql(NAMESPACE + ".selectTeacherStudentLocationById")).doesNotContain("STATUS = 'Y'");
        assertThat(sql(NAMESPACE + ".lockTeacherStudentLocationById")).contains("FOR UPDATE WAIT 3");
    }

    @Test
    void activeViewJoinsActiveLocationWithoutSelectStar() {
        ResultMap resultMap = configuration.getResultMap(NAMESPACE + ".teacherStudentLocationViewResultMap");
        assertThat(resultMap.getResultMappings()).extracting(ResultMapping::getProperty)
                .containsExactlyInAnyOrder("teacherStudentLocationId", "locationId", "locationName", "address");

        String byId = sql(NAMESPACE + ".selectActiveViewById");
        String byRelation = sql(NAMESPACE + ".selectActiveViewsByTeacherStudentId");
        for (String query : List.of(byId, byRelation)) {
            assertThat(query).contains("JOIN TB_LOCATION");
            assertThat(query).contains("tsl.STATUS = 'Y'", "tsl.DELETED_AT IS NULL");
            assertThat(query).contains("loc.STATUS = 'Y'", "loc.DELETED_AT IS NULL");
            assertThat(query).doesNotContain("SELECT *");
        }
        assertThat(byRelation).contains("ORDER BY tsl.CREATED_AT ASC, tsl.TEACHER_STUDENT_LOCATION_ID ASC");
    }

    private static String sql(String statementId) {
        return configuration.getMappedStatement(statementId).getSqlSource().getBoundSql(null).getSql();
    }

    private static String mapperXml() throws java.io.IOException {
        try (InputStream inputStream = TeacherStudentLocationMapperTest.class.getClassLoader()
                .getResourceAsStream("mapper/relationship/TeacherStudentLocationMapper.xml")) {
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
