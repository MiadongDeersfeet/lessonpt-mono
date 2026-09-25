package com.yunki.lessonpt.relationship.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import com.yunki.lessonpt.relationship.domain.Homework;
import com.yunki.lessonpt.relationship.mapper.HomeworkMapper;
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.StudentEmailVerificationMapper;
import com.yunki.lessonpt.relationship.mapper.StudentLoginVerificationMapper;
import com.yunki.lessonpt.relationship.mapper.StudentAccessSessionMapper;
import com.yunki.lessonpt.student.mapper.StudentPortalMapper;
import com.yunki.lessonpt.relationship.mapper.StudentLearningQueryMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.relationship.service.HomeworkChange;
import com.yunki.lessonpt.relationship.service.HomeworkService;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = {
        "spring.profiles.active=context",
        "lessonpt.jwt.secret=01234567890123456789012345678901"
})
@AutoConfigureMockMvc
class HomeworkControllerSecurityTest {

    private static final String BASE = "/api/v1/monitorings/70/homeworks";
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 10, 1, 18, 0);

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
    private StudentLoginVerificationMapper studentLoginVerificationMapper;

    @MockitoBean
    private StudentAccessSessionMapper studentAccessSessionMapper;

    @MockitoBean
    private StudentPortalMapper studentPortalMapper;

    @MockitoBean
    private StudentLearningQueryMapper studentLearningQueryMapper;

    @MockitoBean
    private HomeworkService homeworkService;

    private String bearerToken;

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                {"homeworkContent":"싱글 스트로크"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/90")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(BASE + "/90").contentType(MediaType.APPLICATION_JSON).content("""
                {"feedback":"수정"}
                """)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/90")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/90/restore")).andExpect(status().isUnauthorized());
    }

    @Test
    void createReturnsCreatedHomeworkForPrincipal() throws Exception {
        authenticate(21L);
        when(homeworkService.createHomework(21L, 70L, "싱글 스트로크 연습", DEADLINE, "좋음"))
                .thenReturn(homework(90L, "싱글 스트로크 연습", DEADLINE, false, "좋음"));

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeworkContent":"싱글 스트로크 연습","deadline":"2026-10-01T18:00:00","feedback":"좋음"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString(BASE + "/90")))
                .andExpect(jsonPath("$.homeworkId").value(90))
                .andExpect(jsonPath("$.homeworkContent").value("싱글 스트로크 연습"))
                .andExpect(jsonPath("$.deadline").value("2026-10-01T18:00:00"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.feedback").value("좋음"))
                .andExpect(jsonPath("$.teacherId").doesNotExist())
                .andExpect(jsonPath("$.monitoringId").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        verify(homeworkService).createHomework(21L, 70L, "싱글 스트로크 연습", DEADLINE, "좋음");
    }

    @Test
    void createValidatesContentAndAllowsAnotherHomework() throws Exception {
        authenticate(21L);

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("homeworkContent"));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeworkContent":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("homeworkContent"));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeworkContent":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"));

        when(homeworkService.createHomework(eq(21L), eq(70L), eq("싱글 스트로크"), isNull(), isNull()))
                .thenReturn(homework(90L, "싱글 스트로크", null, false, null));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeworkContent":"싱글 스트로크","deadline":null,"feedback":null}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.deadline").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.feedback").value(org.hamcrest.Matchers.nullValue()));
        verify(homeworkService).createHomework(21L, 70L, "싱글 스트로크", null, null);

        when(homeworkService.createHomework(21L, 70L, "더블 스트로크", null, null))
                .thenReturn(homework(91L, "더블 스트로크", null, false, null));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeworkContent":"더블 스트로크"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.homeworkId").value(91));
    }

    @Test
    void listReturnsHomeworksOrEmptyArray() throws Exception {
        authenticate(21L);
        when(homeworkService.getHomeworks(21L, 70L))
                .thenReturn(List.of(homework(90L, "싱글 스트로크", null, false, null)));

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].homeworkId").value(90))
                .andExpect(jsonPath("$[0].completed").value(false))
                .andExpect(jsonPath("$[0].monitoringId").doesNotExist());

        when(homeworkService.getHomeworks(21L, 70L)).thenReturn(List.of());
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void detailMapsNotFoundToErrorResponse() throws Exception {
        authenticate(21L);
        when(homeworkService.getHomework(21L, 70L, 90L))
                .thenReturn(homework(90L, "싱글 스트로크 10분", DEADLINE, true, "좋음"));

        mockMvc.perform(get(BASE + "/90").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeworkContent").value("싱글 스트로크 10분"))
                .andExpect(jsonPath("$.monitoringId").doesNotExist());

        when(homeworkService.getHomework(21L, 70L, 99L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        mockMvc.perform(get(BASE + "/99").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(BASE + "/99"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void updatePreservesSpecifiedFlagsAndRejectsInvalidValues() throws Exception {
        authenticate(21L);
        when(homeworkService.updateHomework(eq(21L), eq(70L), eq(90L), any()))
                .thenReturn(homework(90L, "수정", DEADLINE, true, "좋음"));

        patchBody("""
                {"homeworkContent":"수정"}
                """);
        patchBody("""
                {"deadline":"2026-10-01T18:00:00"}
                """);
        patchBody("""
                {"deadline":null}
                """);
        patchBody("""
                {"completed":true}
                """);
        patchBody("""
                {"completed":false}
                """);
        patchBody("""
                {"feedback":"좋음"}
                """);
        patchBody("""
                {"feedback":null}
                """);
        patchBody("""
                {}
                """);

        ArgumentCaptor<HomeworkChange> captor = ArgumentCaptor.forClass(HomeworkChange.class);
        verify(homeworkService, times(8)).updateHomework(eq(21L), eq(70L), eq(90L), captor.capture());
        HomeworkChange content = captor.getAllValues().get(0);
        org.assertj.core.api.Assertions.assertThat(content.isHomeworkContentSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(content.getHomeworkContent()).isEqualTo("수정");
        org.assertj.core.api.Assertions.assertThat(content.isDeadlineSpecified()).isFalse();
        org.assertj.core.api.Assertions.assertThat(content.isCompletedSpecified()).isFalse();
        org.assertj.core.api.Assertions.assertThat(content.isFeedbackSpecified()).isFalse();

        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(1).isDeadlineSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(1).getDeadline()).isEqualTo(DEADLINE);
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(2).isDeadlineSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(2).getDeadline()).isNull();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(3).isCompletedSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(3).getCompleted()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(4).isCompletedSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(4).getCompleted()).isFalse();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(5).isFeedbackSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(5).getFeedback()).isEqualTo("좋음");
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(6).isFeedbackSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(6).getFeedback()).isNull();

        HomeworkChange empty = captor.getAllValues().get(7);
        org.assertj.core.api.Assertions.assertThat(empty.isHomeworkContentSpecified()).isFalse();
        org.assertj.core.api.Assertions.assertThat(empty.isDeadlineSpecified()).isFalse();
        org.assertj.core.api.Assertions.assertThat(empty.isCompletedSpecified()).isFalse();
        org.assertj.core.api.Assertions.assertThat(empty.isFeedbackSpecified()).isFalse();

        mockMvc.perform(patch(BASE + "/90")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeworkContent":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("homeworkContent"));
        mockMvc.perform(patch(BASE + "/90")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeworkContent":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("homeworkContent"));
        mockMvc.perform(patch(BASE + "/90")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"completed":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("completed"));
        verify(homeworkService, times(8)).updateHomework(eq(21L), eq(70L), eq(90L), any());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        authenticate(21L);

        mockMvc.perform(delete(BASE + "/90").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(homeworkService).deleteHomework(21L, 70L, 90L);
        verify(homeworkMapper, never()).softDeleteHomework(any(), any());
    }

    @Test
    void restoreReturnsPreservedRowAndMapsConflictOrNotFound() throws Exception {
        authenticate(21L);
        when(homeworkService.restoreHomework(21L, 70L, 90L))
                .thenReturn(homework(90L, "싱글 스트로크 10분", DEADLINE, true, "좋음"));

        mockMvc.perform(post(BASE + "/90/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeworkContent").value("싱글 스트로크 10분"))
                .andExpect(jsonPath("$.deadline").value("2026-10-01T18:00:00"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.feedback").value("좋음"))
                .andExpect(jsonPath("$.monitoringId").doesNotExist());

        when(homeworkService.restoreHomework(21L, 70L, 91L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_CONFLICT));
        mockMvc.perform(post(BASE + "/91/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMMON_CONFLICT"));

        when(homeworkService.restoreHomework(21L, 70L, 92L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        mockMvc.perform(post(BASE + "/92/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value(BASE + "/92/restore"));
    }

    private void patchBody(String body) throws Exception {
        mockMvc.perform(patch(BASE + "/90")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").exists());
    }

    private Homework homework(
            Long homeworkId, String content, LocalDateTime deadline, boolean completed, String feedback) {
        Homework homework = new Homework();
        homework.setHomeworkId(homeworkId);
        homework.setMonitoringId(70L);
        homework.setHomeworkContent(content);
        homework.setDeadline(deadline);
        homework.setCompleted(completed);
        homework.setFeedback(feedback);
        homework.setStatus(RecordStatus.ACTIVE);
        return homework;
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
