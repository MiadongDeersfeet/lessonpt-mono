package com.yunki.lessonpt.relationship.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.mapper.HomeworkMapper;
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.StudentEmailVerificationMapper;
import com.yunki.lessonpt.relationship.mapper.StudentAccessSessionMapper;
import com.yunki.lessonpt.student.mapper.StudentPortalMapper;
import com.yunki.lessonpt.relationship.mapper.StudentLearningQueryMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.relationship.dto.ProgressSummary;
import com.yunki.lessonpt.relationship.dto.StudentCurriculumProgressResult;
import com.yunki.lessonpt.relationship.service.ProgressQueryService;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = {
        "spring.profiles.active=context",
        "lessonpt.jwt.secret=01234567890123456789012345678901"
})
@AutoConfigureMockMvc
class ProgressQueryControllerSecurityTest {

    private static final String SINGLE = "/api/v1/student-curriculums/42/progress";
    private static final String BATCH = "/api/v1/student-curriculums/progress/query";

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
    private ProgressQueryMapper progressQueryMapper;

    @MockitoBean
    private TeacherStudentAccessMapper teacherStudentAccessMapper;

    @MockitoBean
    private StudentEmailVerificationMapper studentEmailVerificationMapper;

    @MockitoBean
    private StudentAccessSessionMapper studentAccessSessionMapper;

    @MockitoBean
    private StudentPortalMapper studentPortalMapper;

    @MockitoBean
    private StudentLearningQueryMapper studentLearningQueryMapper;

    @MockitoBean
    private ProgressQueryService progressQueryService;

    private String bearerToken;

    @Test
    void progressRequiresAuthentication() throws Exception {
        mockMvc.perform(get(SINGLE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
        mockMvc.perform(post(BATCH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentCurriculumIds":[10]}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void getReturnsServicePercentageWithoutRecalculating() throws Exception {
        authenticate(21L);
        when(progressQueryService.getProgress(21L, 42L))
                .thenReturn(progress(42L, 1, 3, "33.3"));

        mockMvc.perform(get(SINGLE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentCurriculumId").value(42))
                .andExpect(jsonPath("$.progress.completedCount").value(1))
                .andExpect(jsonPath("$.progress.totalCount").value(3))
                .andExpect(jsonPath("$.progress.percentage").value(33.3))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"percentage\":33.3")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\"33.3\""))));

        when(progressQueryService.getProgress(21L, 42L))
                .thenReturn(progress(42L, 4, 4, "100.0"));
        mockMvc.perform(get(SINGLE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"percentage\":100.0")));

        verify(progressQueryService, org.mockito.Mockito.times(2)).getProgress(21L, 42L);
        verify(progressQueryMapper, never()).selectProgressByStudentCurriculumId(any());
    }

    @Test
    void getReturnsNullProgressWhenThereIsNoActiveContent() throws Exception {
        authenticate(21L);
        when(progressQueryService.getProgress(21L, 42L))
                .thenReturn(new StudentCurriculumProgressResult(42L, null));

        mockMvc.perform(get(SINGLE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentCurriculumId").value(42))
                .andExpect(jsonPath("$.progress").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getMapsNotFoundAndInternalError() throws Exception {
        authenticate(21L);
        doThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND))
                .when(progressQueryService).getProgress(21L, 42L);
        mockMvc.perform(get(SINGLE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(SINGLE))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());

        doThrow(new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR))
                .when(progressQueryService).getProgress(21L, 42L);
        mockMvc.perform(get(SINGLE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("COMMON_INTERNAL_ERROR"))
                .andExpect(jsonPath("$.path").value(SINGLE))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void queryKeepsZeroTotalIdsWithNullProgress() throws Exception {
        authenticate(21L);
        when(progressQueryService.getProgresses(21L, List.of(10L, 11L, 12L)))
                .thenReturn(List.of(
                        progress(10L, 2, 4, "50.0"),
                        new StudentCurriculumProgressResult(11L, null),
                        progress(12L, 2, 3, "12.5")));

        mockMvc.perform(post(BATCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentCurriculumIds":[10,11,12]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].studentCurriculumId").value(10))
                .andExpect(jsonPath("$[0].progress.completedCount").value(2))
                .andExpect(jsonPath("$[0].progress.totalCount").value(4))
                .andExpect(jsonPath("$[0].progress.percentage").value(50.0))
                .andExpect(jsonPath("$[1].studentCurriculumId").value(11))
                .andExpect(jsonPath("$[1].progress").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[2].studentCurriculumId").value(12))
                .andExpect(jsonPath("$[2].progress.percentage").value(12.5));

        verify(progressQueryService).getProgresses(21L, List.of(10L, 11L, 12L));
        verify(progressQueryMapper, never()).selectProgressByStudentCurriculumIds(any());
    }

    @Test
    void queryAllowsEmptyAndDuplicateIds() throws Exception {
        authenticate(21L);
        when(progressQueryService.getProgresses(21L, List.of())).thenReturn(List.of());
        mockMvc.perform(post(BATCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentCurriculumIds":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        when(progressQueryService.getProgresses(21L, List.of(10L, 10L, 20L)))
                .thenReturn(List.of(progress(10L, 1, 4, "25.0")));
        mockMvc.perform(post(BATCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentCurriculumIds":[10,10,20]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentCurriculumId").value(10));
        verify(progressQueryService).getProgresses(21L, List.of(10L, 10L, 20L));
    }

    @Test
    void queryRejectsInvalidIds() throws Exception {
        authenticate(21L);
        mockMvc.perform(post(BATCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("studentCurriculumIds"));
        mockMvc.perform(post(BATCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentCurriculumIds":[10,null]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"));
        mockMvc.perform(post(BATCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentCurriculumIds":[0]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"));
        mockMvc.perform(post(BATCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentCurriculumIds":[-1]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"));
        verify(progressQueryService, never()).getProgresses(any(), any());
    }

    @Test
    void queryMapsNotFound() throws Exception {
        authenticate(21L);
        doThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND))
                .when(progressQueryService).getProgresses(21L, List.of(10L, 11L));

        mockMvc.perform(post(BATCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentCurriculumIds":[10,11]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value(BATCH));
    }

    private static StudentCurriculumProgressResult progress(
            Long studentCurriculumId, int completedCount, int totalCount, String percentage) {
        return new StudentCurriculumProgressResult(
                studentCurriculumId,
                new ProgressSummary(completedCount, totalCount, new BigDecimal(percentage)));
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
