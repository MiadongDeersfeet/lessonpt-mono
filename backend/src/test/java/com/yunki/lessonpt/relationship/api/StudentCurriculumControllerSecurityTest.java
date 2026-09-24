package com.yunki.lessonpt.relationship.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.HomeworkMapper;
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.relationship.service.StudentCurriculumChange;
import com.yunki.lessonpt.relationship.service.StudentCurriculumService;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = {
        "spring.profiles.active=context",
        "lessonpt.jwt.secret=01234567890123456789012345678901"
})
@AutoConfigureMockMvc
class StudentCurriculumControllerSecurityTest {

    private static final String BASE = "/api/v1/student-locations/30/curriculums";

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
    private StudentCurriculumService studentCurriculumService;

    private String bearerToken;

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                {"curriculumId":7}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/41")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(BASE + "/41").contentType(MediaType.APPLICATION_JSON).content("""
                {"memo":"수정"}
                """)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/41")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/41/restore")).andExpect(status().isUnauthorized());
    }

    @Test
    void assignReturnsCreatedEnrollmentForPrincipal() throws Exception {
        authenticate(21L);
        when(studentCurriculumService.assignStudentCurriculum(21L, 30L, 7L, "기초 루디먼트부터 시작"))
                .thenReturn(enrollment(41L, 7L, false, "기초 루디먼트부터 시작"));

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"curriculumId":7,"memo":"기초 루디먼트부터 시작"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString(BASE + "/41")))
                .andExpect(jsonPath("$.studentCurriculumId").value(41))
                .andExpect(jsonPath("$.curriculumId").value(7))
                .andExpect(jsonPath("$.reenrolled").value(false))
                .andExpect(jsonPath("$.memo").value("기초 루디먼트부터 시작"))
                .andExpect(jsonPath("$.teacherId").doesNotExist())
                .andExpect(jsonPath("$.teacherStudentLocationId").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        verify(studentCurriculumService).assignStudentCurriculum(21L, 30L, 7L, "기초 루디먼트부터 시작");
    }

    @Test
    void assignRejectsMissingCurriculumAndMapsConflictOrReenroll() throws Exception {
        authenticate(21L);

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memo":"메모"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("curriculumId"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        when(studentCurriculumService.assignStudentCurriculum(21L, 30L, 7L, null))
                .thenThrow(new BusinessException(ErrorCode.COMMON_CONFLICT));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"curriculumId":7}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMON_CONFLICT"))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        when(studentCurriculumService.assignStudentCurriculum(21L, 30L, 8L, "기존 메모"))
                .thenReturn(enrollment(41L, 8L, true, "기존 메모"));
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"curriculumId":8,"memo":"기존 메모"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reenrolled").value(true))
                .andExpect(jsonPath("$.memo").value("기존 메모"));
    }

    @Test
    void listReturnsEnrollmentsOrEmptyArray() throws Exception {
        authenticate(21L);
        when(studentCurriculumService.getStudentCurriculums(21L, 30L))
                .thenReturn(List.of(enrollment(41L, 7L, false, "기초")));

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentCurriculumId").value(41))
                .andExpect(jsonPath("$[0].curriculumId").value(7));

        when(studentCurriculumService.getStudentCurriculums(21L, 30L)).thenReturn(List.of());
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void detailMapsNotFoundToErrorResponse() throws Exception {
        authenticate(21L);
        when(studentCurriculumService.getStudentCurriculum(21L, 30L, 41L))
                .thenReturn(enrollment(41L, 7L, false, "기초"));

        mockMvc.perform(get(BASE + "/41").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculumId").value(7));

        when(studentCurriculumService.getStudentCurriculum(21L, 30L, 42L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        mockMvc.perform(get(BASE + "/42").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(BASE + "/42"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void updatePreservesMemoSpecifiedFlag() throws Exception {
        authenticate(21L);
        when(studentCurriculumService.updateStudentCurriculum(eq(21L), eq(30L), eq(41L), any()))
                .thenReturn(enrollment(41L, 7L, false, "재수강 시 집중 연습"));

        patchBody("""
                {"memo":"재수강 시 집중 연습"}
                """);
        patchBody("""
                {"memo":null}
                """);
        patchBody("""
                {}
                """);

        ArgumentCaptor<StudentCurriculumChange> captor = ArgumentCaptor.forClass(StudentCurriculumChange.class);
        verify(studentCurriculumService, times(3))
                .updateStudentCurriculum(eq(21L), eq(30L), eq(41L), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(0).isMemoSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(0).getMemo()).isEqualTo("재수강 시 집중 연습");
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(1).isMemoSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(1).getMemo()).isNull();
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(2).isMemoSpecified()).isFalse();

        when(studentCurriculumService.updateStudentCurriculum(eq(21L), eq(31L), eq(41L), any()))
                .thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        mockMvc.perform(patch("/api/v1/student-locations/31/curriculums/41")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memo":"다른장소"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        authenticate(21L);

        mockMvc.perform(delete(BASE + "/41").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(studentCurriculumService).deleteStudentCurriculum(21L, 30L, 41L);
    }

    @Test
    void restoreReturnsReenrolledRowAndMapsConflictOrNotFound() throws Exception {
        authenticate(21L);
        when(studentCurriculumService.restoreStudentCurriculum(21L, 30L, 41L))
                .thenReturn(enrollment(41L, 7L, true, "기초 루디먼트부터 시작"));

        mockMvc.perform(post(BASE + "/41/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentCurriculumId").value(41))
                .andExpect(jsonPath("$.reenrolled").value(true))
                .andExpect(jsonPath("$.teacherStudentLocationId").doesNotExist());

        when(studentCurriculumService.restoreStudentCurriculum(21L, 30L, 42L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_CONFLICT));
        mockMvc.perform(post(BASE + "/42/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMMON_CONFLICT"))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        when(studentCurriculumService.restoreStudentCurriculum(21L, 30L, 43L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        mockMvc.perform(post(BASE + "/43/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value(BASE + "/43/restore"));
    }

    private void patchBody(String body) throws Exception {
        mockMvc.perform(patch(BASE + "/41")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private StudentCurriculum enrollment(Long studentCurriculumId, Long curriculumId, boolean reenrolled, String memo) {
        StudentCurriculum studentCurriculum = new StudentCurriculum();
        studentCurriculum.setStudentCurriculumId(studentCurriculumId);
        studentCurriculum.setTeacherStudentLocationId(30L);
        studentCurriculum.setCurriculumId(curriculumId);
        studentCurriculum.setReenrolled(reenrolled);
        studentCurriculum.setMemo(memo);
        studentCurriculum.setStatus(RecordStatus.ACTIVE);
        return studentCurriculum;
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
