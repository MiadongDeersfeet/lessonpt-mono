package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.yunki.lessonpt.common.mybatis.typehandler.StudentEmailVerificationStatusTypeHandler;

class StudentEmailVerificationMapperTest {

    private static final String NAMESPACE = "com.yunki.lessonpt.relationship.mapper.StudentEmailVerificationMapper";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() {
        configuration = new Configuration();
        String resource = "mapper/relationship/StudentEmailVerificationMapper.xml";
        InputStream inputStream = StudentEmailVerificationMapperTest.class.getClassLoader().getResourceAsStream(resource);
        assertThat(inputStream).isNotNull();
        new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
    }

    @Test
    void resultMapAndStatementsStayExplicit() throws Exception {
        ResultMap resultMap = configuration.getResultMap(NAMESPACE + ".studentEmailVerificationResultMap");
        assertThat(resultMap.getResultMappings()).extracting(ResultMapping::getProperty)
                .containsExactlyInAnyOrder(
                        "verificationId", "teacherStudentAccessId", "emailHash", "codeHash",
                        "verificationStatus", "failedAttemptCount", "expiresAt", "consumedAt",
                        "invalidatedAt", "lockedUntil", "createdAt", "updatedAt");
        assertThat(mapping(resultMap, "verificationStatus").getTypeHandler())
                .isInstanceOf(StudentEmailVerificationStatusTypeHandler.class);
        String xml = mapperXml();
        assertThat(xml).doesNotContain("SELECT *").doesNotContain("${").doesNotContain("<delete");
        assertThat(sql(NAMESPACE + ".selectCurrentPendingByAccessId")).contains(
                "VERIFICATION_STATUS = 'PENDING'", "CONSUMED_AT IS NULL", "INVALIDATED_AT IS NULL", "LOCKED_UNTIL IS NULL");
        assertThat(sql(NAMESPACE + ".invalidatePendingByAccessId")).contains("VERIFICATION_STATUS = 'INVALIDATED'");
        assertThat(sql(NAMESPACE + ".markConsumed")).contains("VERIFICATION_STATUS = 'CONSUMED'", "CONSUMED_AT = ?");
        assertThat(sql(NAMESPACE + ".markLocked")).contains("VERIFICATION_STATUS = 'LOCKED'", "LOCKED_UNTIL = ?", "FAILED_ATTEMPT_COUNT = 5");
        assertThat(sql(NAMESPACE + ".insertStudentEmailVerification")).doesNotContain("DELETE FROM");
        assertThat(configuration.getMappedStatement(NAMESPACE + ".insertStudentEmailVerification!selectKey")
                .getSqlSource().getBoundSql(null).getSql()).contains("SEQ_STUDENT_EMAIL_VERIFICATION.NEXTVAL");
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
        try (InputStream inputStream = StudentEmailVerificationMapperTest.class.getClassLoader()
                .getResourceAsStream("mapper/relationship/StudentEmailVerificationMapper.xml")) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
