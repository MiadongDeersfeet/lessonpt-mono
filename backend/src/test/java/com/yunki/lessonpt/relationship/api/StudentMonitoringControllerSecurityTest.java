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
import com.yunki.lessonpt.common.model.ProgressStatus;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.domain.StudentMonitoring;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.HomeworkMapper;
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.StudentEmailVerificationMapper;
import com.yunki.lessonpt.relationship.mapper.StudentLoginVerificationMapper;
import com.yunki.lessonpt.relationship.mapper.StudentAccessSessionMapper;
import com.yunki.lessonpt.student.mapper.StudentPortalMapper;
import com.yunki.lessonpt.relationship.mapper.StudentLearningQueryMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.relationship.service.StudentMonitoringChange;
import com.yunki.lessonpt.relationship.service.StudentMonitoringService;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = {
        "spring.profiles.active=context",
        "lessonpt.jwt.secret=01234567890123456789012345678901"
})
@AutoConfigureMockMvc
class StudentMonitoringControllerSecurityTest {

    private static final String BASE = "/api/v1/student-curriculums/50/monitorings";

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
    private StudentMonitoringService studentMonitoringService;

    private String bearerToken;

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                {"contentDetailId":18}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/55")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(BASE + "/55").contentType(MediaType.APPLICATION_JSON).content("""
                {"memo":"수정"}
                """)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/55")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/55/restore")).andExpect(status().isUnauthorized());
    }

    @Test
    void assignReturnsCreatedMonitoringForPrincipal() throws Exception {
        authenticate(21L);
        when(studentMonitoringService.assignStudentMonitoring(21L, 50L, 18L, 120, ProgressStatus.IN_PROGRESS, "오른손 힘 빼기"))
                .thenReturn(monitoring(55L, 18L, 2, 120, ProgressStatus.IN_PROGRESS, "오른손 힘 빼기"));

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentDetailId":18,"currentBpm":120,"progressStatus":"IN_PROGRESS","memo":"오른손 힘 빼기"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString(BASE + "/55")))
                .andExpect(jsonPath("$.monitoringId").value(55))
                .andExpect(jsonPath("$.contentDetailId").value(18))
                .andExpect(jsonPath("$.displayOrder").value(2))
                .andExpect(jsonPath("$.currentBpm").value(120))
                .andExpect(jsonPath("$.progressStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.memo").value("오른손 힘 빼기"))
                .andExpect(jsonPath("$.teacherId").doesNotExist())
                .andExpect(jsonPath("$.studentCurriculumId").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        verify(studentMonitoringService).assignStudentMonitoring(
                21L, 50L, 18L, 120, ProgressStatus.IN_PROGRESS, "오른손 힘 빼기");
    }

    @Test
    void assignValidatesInputAndMapsConflictOrRestore() throws Exception {
        authenticate(21L);

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentBpm":120}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("contentDetailId"));

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentDetailId":18,"currentBpm":59}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currentBpm"));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentDetailId":18,"currentBpm":241}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"));

        when(studentMonitoringService.assignStudentMonitoring(eq(21L), eq(50L), eq(18L), isNull(), isNull(), isNull()))
                .thenReturn(monitoring(55L, 18L, 1, null, ProgressStatus.YET, null));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentDetailId":18}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentBpm").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.progressStatus").value("YET"));
        verify(studentMonitoringService).assignStudentMonitoring(21L, 50L, 18L, null, null, null);

        when(studentMonitoringService.assignStudentMonitoring(21L, 50L, 18L, 60, null, null))
                .thenReturn(monitoring(56L, 18L, 1, 60, ProgressStatus.YET, null));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentDetailId":18,"currentBpm":60}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentBpm").value(60));

        when(studentMonitoringService.assignStudentMonitoring(21L, 50L, 18L, 240, null, null))
                .thenReturn(monitoring(57L, 18L, 1, 240, ProgressStatus.YET, null));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentDetailId":18,"currentBpm":240}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentBpm").value(240));

        when(studentMonitoringService.assignStudentMonitoring(21L, 50L, 19L, null, null, null))
                .thenThrow(new BusinessException(ErrorCode.COMMON_CONFLICT));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentDetailId":19}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMON_CONFLICT"));

        when(studentMonitoringService.assignStudentMonitoring(21L, 50L, 20L, null, ProgressStatus.YET, "새 메모"))
                .thenReturn(monitoring(58L, 20L, 4, 130, ProgressStatus.COMPLETED, "완료"));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentDetailId":20,"progressStatus":"YET","memo":"새 메모"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentBpm").value(130))
                .andExpect(jsonPath("$.progressStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.memo").value("완료"))
                .andExpect(jsonPath("$.displayOrder").value(4));
    }

    @Test
    void listReturnsMonitoringsOrEmptyArray() throws Exception {
        authenticate(21L);
        when(studentMonitoringService.getStudentMonitorings(21L, 50L))
                .thenReturn(List.of(monitoring(55L, 18L, 1, null, ProgressStatus.YET, null)));

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monitoringId").value(55))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                .andExpect(jsonPath("$[0].progressStatus").value("YET"));

        when(studentMonitoringService.getStudentMonitorings(21L, 50L)).thenReturn(List.of());
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void detailMapsNotFoundToErrorResponse() throws Exception {
        authenticate(21L);
        when(studentMonitoringService.getStudentMonitoring(21L, 50L, 55L))
                .thenReturn(monitoring(55L, 18L, 2, 120, ProgressStatus.IN_PROGRESS, "오른손 힘 빼기"));

        mockMvc.perform(get(BASE + "/55").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentDetailId").value(18))
                .andExpect(jsonPath("$.studentCurriculumId").doesNotExist());

        when(studentMonitoringService.getStudentMonitoring(21L, 50L, 99L))
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
        when(studentMonitoringService.updateStudentMonitoring(eq(21L), eq(50L), eq(55L), any()))
                .thenReturn(monitoring(55L, 18L, 2, 120, ProgressStatus.COMPLETED, "수정"));

        patchBody("""
                {"currentBpm":120,"progressStatus":"COMPLETED","memo":"수정"}
                """);
        patchBody("""
                {"currentBpm":null,"memo":null}
                """);
        patchBody("""
                {}
                """);
        patchBody("""
                {"progressStatus":"YET"}
                """);

        ArgumentCaptor<StudentMonitoringChange> captor = ArgumentCaptor.forClass(StudentMonitoringChange.class);
        verify(studentMonitoringService, times(4))
                .updateStudentMonitoring(eq(21L), eq(50L), eq(55L), captor.capture());
        StudentMonitoringChange values = captor.getAllValues().get(0);
        org.assertj.core.api.Assertions.assertThat(values.isCurrentBpmSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(values.getCurrentBpm()).isEqualTo(120);
        org.assertj.core.api.Assertions.assertThat(values.isProgressStatusSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(values.getProgressStatus()).isEqualTo(ProgressStatus.COMPLETED);
        org.assertj.core.api.Assertions.assertThat(values.isMemoSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(values.getMemo()).isEqualTo("수정");

        StudentMonitoringChange cleared = captor.getAllValues().get(1);
        org.assertj.core.api.Assertions.assertThat(cleared.isCurrentBpmSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(cleared.getCurrentBpm()).isNull();
        org.assertj.core.api.Assertions.assertThat(cleared.isMemoSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(cleared.getMemo()).isNull();
        org.assertj.core.api.Assertions.assertThat(cleared.isProgressStatusSpecified()).isFalse();

        StudentMonitoringChange empty = captor.getAllValues().get(2);
        org.assertj.core.api.Assertions.assertThat(empty.isCurrentBpmSpecified()).isFalse();
        org.assertj.core.api.Assertions.assertThat(empty.isProgressStatusSpecified()).isFalse();
        org.assertj.core.api.Assertions.assertThat(empty.isMemoSpecified()).isFalse();

        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(3).isProgressStatusSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(3).getProgressStatus())
                .isEqualTo(ProgressStatus.YET);
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(3).isCurrentBpmSpecified()).isFalse();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(3).isMemoSpecified()).isFalse();

        mockMvc.perform(patch(BASE + "/55")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"progressStatus":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("progressStatus"));
        mockMvc.perform(patch(BASE + "/55")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentBpm":59}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currentBpm"));
        mockMvc.perform(patch(BASE + "/55")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentBpm":241}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"));
        verify(studentMonitoringService, times(4)).updateStudentMonitoring(eq(21L), eq(50L), eq(55L), any());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        authenticate(21L);

        mockMvc.perform(delete(BASE + "/55").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(studentMonitoringService).deleteStudentMonitoring(21L, 50L, 55L);
        verify(studentMonitoringMapper, never()).softDeleteStudentMonitoring(any(), any());
    }

    @Test
    void restoreReturnsPreservedRowAndMapsConflictOrNotFound() throws Exception {
        authenticate(21L);
        when(studentMonitoringService.restoreStudentMonitoring(21L, 50L, 55L))
                .thenReturn(monitoring(55L, 18L, 4, 130, ProgressStatus.COMPLETED, "완료"));

        mockMvc.perform(post(BASE + "/55/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBpm").value(130))
                .andExpect(jsonPath("$.progressStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.memo").value("완료"))
                .andExpect(jsonPath("$.displayOrder").value(4))
                .andExpect(jsonPath("$.studentCurriculumId").doesNotExist());

        when(studentMonitoringService.restoreStudentMonitoring(21L, 50L, 56L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_CONFLICT));
        mockMvc.perform(post(BASE + "/56/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMMON_CONFLICT"));

        when(studentMonitoringService.restoreStudentMonitoring(21L, 50L, 57L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        mockMvc.perform(post(BASE + "/57/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value(BASE + "/57/restore"));
    }

    private void patchBody(String body) throws Exception {
        mockMvc.perform(patch(BASE + "/55")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayOrder").value(2));
    }

    private StudentMonitoring monitoring(
            Long monitoringId,
            Long contentDetailId,
            int displayOrder,
            Integer currentBpm,
            ProgressStatus progressStatus,
            String memo) {
        StudentMonitoring monitoring = new StudentMonitoring();
        monitoring.setMonitoringId(monitoringId);
        monitoring.setStudentCurriculumId(50L);
        monitoring.setContentDetailId(contentDetailId);
        monitoring.setDisplayOrder(displayOrder);
        monitoring.setCurrentBpm(currentBpm);
        monitoring.setProgressStatus(progressStatus);
        monitoring.setMemo(memo);
        monitoring.setStatus(RecordStatus.ACTIVE);
        return monitoring;
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
