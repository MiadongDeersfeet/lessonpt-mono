package com.yunki.lessonpt;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.yunki.lessonpt.common.diagnostic.SchemaV3Tables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.yunki.lessonpt.auth.mapper.TeacherAuthSessionMapper;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 접속 정보 없이 Spring 이 뜨는지, health로 Oracle 미연결을 구분하는지만 본다.
 * local 프로파일을 켜지 않아서 개발 Schema에는 붙지 않는다.
 */
@SpringBootTest(properties = "spring.profiles.active=context")
@AutoConfigureMockMvc
class LessonPtApplicationTests {

    @MockitoBean
    private TeacherMapper teacherMapper;

    @MockitoBean
    private CurriculumMapper curriculumMapper;

    @MockitoBean
    private CategoryMapper categoryMapper;

    @MockitoBean
    private ContentDetailMapper contentDetailMapper;

    @MockitoBean
    private StudentCurriculumMapper studentCurriculumMapper;

    @MockitoBean
    private StudentMonitoringMapper studentMonitoringMapper;

    @MockitoBean
    private TeacherAuthSessionMapper teacherAuthSessionMapper;

    @MockitoBean
    private LocationMapper locationMapper;

    @MockitoBean
    private StudentMapper studentMapper;

    @MockitoBean
    private TeacherStudentMapper teacherStudentMapper;

    @MockitoBean
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthShowsOracleDownWhenDatasourceIsMissing() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("DOWN")))
                .andExpect(content().string(containsString("oracle")))
                .andExpect(content().string(containsString("Oracle 접속 정보가 없다")));
    }

    @Test
    void smokeMapperXmlMatchesInterface() throws Exception {
        try (var input = getClass().getResourceAsStream("/mapper/common/SmokeMapper.xml")) {
            assertThat(input).isNotNull();
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(xml)
                    .contains("namespace=\"com.yunki.lessonpt.common.health.SmokeMapper\"")
                    .contains("SELECT 1 FROM DUAL");
        }
    }

    @Test
    void diagnosticSqlChecksContextAndNamedTables() throws Exception {
        try (var input = getClass().getResourceAsStream("/mapper/common/OracleDiagnosticMapper.xml")) {
            assertThat(input).isNotNull();
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(xml)
                    .contains("namespace=\"com.yunki.lessonpt.common.diagnostic.OracleDiagnosticMapper\"")
                    .contains("SYS_CONTEXT('USERENV', 'DB_NAME')")
                    .contains("SYS_CONTEXT('USERENV', 'CON_NAME')")
                    .contains("SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')")
                    .contains("FROM USER_TABLES")
                    .contains("collection=\"tableNames\"");
        }
        assertThat(SchemaV3Tables.NAMES).containsExactly(
                "TB_TEACHER",
                "TB_TEACHER_AUTH_SESSION",
                "TB_LOCATION",
                "TB_STUDENT",
                "TB_TEACHER_STUDENT",
                "TB_TEACHER_STUDENT_ACCESS",
                "TB_STUDENT_EMAIL_VERIFICATION",
                "TB_STUDENT_ACCESS_SESSION",
                "TB_TEACHER_STUDENT_LOCATION",
                "TB_CURRICULUM",
                "TB_CATEGORY",
                "TB_CONTENT_DETAIL",
                "TB_STUDENT_CURRICULUM",
                "TB_STUDENT_MONITORING",
                "TB_HOMEWORK");
    }
}
