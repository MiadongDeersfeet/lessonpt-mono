package com.yunki.lessonpt.curriculum.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.yunki.lessonpt.auth.domain.TeacherAuthSession;
import com.yunki.lessonpt.auth.jwt.IssuedToken;
import com.yunki.lessonpt.auth.jwt.JwtProvider;
import com.yunki.lessonpt.auth.mapper.TeacherAuthSessionMapper;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.dto.CurriculumUpdateRequest;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.HomeworkMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.curriculum.service.CurriculumService;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = {
        "spring.profiles.active=context",
        "lessonpt.jwt.secret=01234567890123456789012345678901"
})
@AutoConfigureMockMvc
class CurriculumControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private TeacherMapper teacherMapper;

    @MockitoBean
    private TeacherAuthSessionMapper sessionMapper;

    @MockitoBean
    private LocationMapper locationMapper;

    @MockitoBean
    private StudentMapper studentMapper;

    @MockitoBean
    private TeacherStudentMapper teacherStudentMapper;

    @MockitoBean
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

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
    private HomeworkMapper homeworkMapper;

    @MockitoBean
    private CurriculumService curriculumService;

    private String bearerToken;

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/curriculums").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"기초 드럼"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
        mockMvc.perform(get("/api/v1/curriculums"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/curriculums/50"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/curriculums/50").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"개정"}
                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/curriculums/50"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/curriculums/50/restore"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturnsCreatedCurriculumForPrincipal() throws Exception {
        authenticate(21L);
        when(curriculumService.createCurriculum(21L, "기초 드럼")).thenReturn(curriculum(50L, "기초 드럼", 1));

        mockMvc.perform(post("/api/v1/curriculums")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"기초 드럼"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("/api/v1/curriculums/50")))
                .andExpect(jsonPath("$.curriculumId").value(50))
                .andExpect(jsonPath("$.name").value("기초 드럼"))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.teacherId").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        verify(curriculumService).createCurriculum(21L, "기초 드럼");
    }

    @Test
    void createRejectsBlankAndTooLongName() throws Exception {
        authenticate(21L);

        mockMvc.perform(post("/api/v1/curriculums")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(post("/api/v1/curriculums")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "가".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void listReturnsCurriculumsOrEmptyArray() throws Exception {
        authenticate(21L);
        when(curriculumService.getCurriculums(21L)).thenReturn(List.of(curriculum(50L, "기초 드럼", 1)));

        mockMvc.perform(get("/api/v1/curriculums").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].curriculumId").value(50))
                .andExpect(jsonPath("$[0].name").value("기초 드럼"))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                .andExpect(jsonPath("$[0].teacherId").doesNotExist());

        when(curriculumService.getCurriculums(21L)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/curriculums").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void detailMapsNotFoundToErrorResponse() throws Exception {
        authenticate(21L);
        when(curriculumService.getCurriculum(21L, 50L)).thenReturn(curriculum(50L, "기초 드럼", 1));

        mockMvc.perform(get("/api/v1/curriculums/50").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("기초 드럼"));

        when(curriculumService.getCurriculum(21L, 99L)).thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        mockMvc.perform(get("/api/v1/curriculums/99").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/curriculums/99"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void updateSendsPrincipalAndRejectsBlankOrNullName() throws Exception {
        authenticate(21L);
        when(curriculumService.updateCurriculum(org.mockito.ArgumentMatchers.eq(21L), org.mockito.ArgumentMatchers.eq(50L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(curriculum(50L, "개정", 1));

        mockMvc.perform(patch("/api/v1/curriculums/50")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"개정"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("개정"))
                .andExpect(jsonPath("$.displayOrder").value(1));

        org.mockito.ArgumentCaptor<CurriculumUpdateRequest> captor = org.mockito.ArgumentCaptor.forClass(CurriculumUpdateRequest.class);
        verify(curriculumService).updateCurriculum(org.mockito.ArgumentMatchers.eq(21L), org.mockito.ArgumentMatchers.eq(50L), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().isNameSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getName()).isEqualTo("개정");

        mockMvc.perform(patch("/api/v1/curriculums/50")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

        mockMvc.perform(patch("/api/v1/curriculums/50")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        authenticate(21L);

        mockMvc.perform(delete("/api/v1/curriculums/50").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(curriculumService).deleteCurriculum(21L, 50L);
    }

    @Test
    void restoreReturnsCurriculumAndMapsConflict() throws Exception {
        authenticate(21L);
        when(curriculumService.restoreCurriculum(21L, 50L)).thenReturn(curriculum(50L, "기초 드럼", 4));

        mockMvc.perform(post("/api/v1/curriculums/50/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculumId").value(50))
                .andExpect(jsonPath("$.displayOrder").value(4))
                .andExpect(jsonPath("$.teacherId").doesNotExist());

        when(curriculumService.restoreCurriculum(21L, 51L)).thenThrow(new BusinessException(ErrorCode.COMMON_CONFLICT));
        mockMvc.perform(post("/api/v1/curriculums/51/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMMON_CONFLICT"))
                .andExpect(jsonPath("$.path").value("/api/v1/curriculums/51/restore"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    private Curriculum curriculum(Long curriculumId, String name, int displayOrder) {
        Curriculum curriculum = new Curriculum();
        curriculum.setCurriculumId(curriculumId);
        curriculum.setTeacherId(21L);
        curriculum.setName(name);
        curriculum.setDisplayOrder(displayOrder);
        curriculum.setStatus(RecordStatus.ACTIVE);
        return curriculum;
    }

    private void authenticate(Long teacherId) {
        IssuedToken token = jwtProvider.createAccessToken(teacherId);
        bearerToken = "Bearer " + token.value();
        Teacher teacher = new Teacher();
        teacher.setTeacherId(teacherId);
        teacher.setEmail("teacher@lessonpt.local");
        teacher.setStatus(RecordStatus.ACTIVE);
        TeacherAuthSession session = new TeacherAuthSession();
        session.setTeacherId(teacherId);
        session.setAccessJti(token.jti());
        when(sessionMapper.selectActiveSessionByAccessJti(token.jti())).thenReturn(session);
        when(teacherMapper.selectActiveTeacherById(teacherId)).thenReturn(teacher);
    }
}
