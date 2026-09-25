package com.yunki.lessonpt.student.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
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
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.HomeworkMapper;
import com.yunki.lessonpt.relationship.dto.TeacherStudentAccessResponse;
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.mapper.StudentEmailVerificationMapper;
import com.yunki.lessonpt.relationship.mapper.StudentLoginVerificationMapper;
import com.yunki.lessonpt.relationship.mapper.StudentAccessSessionMapper;
import com.yunki.lessonpt.resource.mapper.ContentResourceMapper;
import com.yunki.lessonpt.student.mapper.StudentPortalMapper;
import com.yunki.lessonpt.relationship.mapper.StudentLearningQueryMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.config.StudentSessionProperties;
import com.yunki.lessonpt.relationship.service.IssuedStudentSession;
import com.yunki.lessonpt.relationship.service.StudentAccessSessionService;
import com.yunki.lessonpt.relationship.mail.EmailDeliveryException;
import com.yunki.lessonpt.relationship.service.StudentEmailVerificationService;
import com.yunki.lessonpt.auth.security.StudentPrincipal;
import com.yunki.lessonpt.relationship.service.TeacherStudentAccessService;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.dto.StudentCreateRequest;
import com.yunki.lessonpt.student.dto.StudentResponse;
import com.yunki.lessonpt.student.dto.StudentLearningResponse;
import com.yunki.lessonpt.student.dto.StudentMeResponse;
import com.yunki.lessonpt.student.service.StudentPortalService;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.student.service.StudentService;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = {
        "spring.profiles.active=context",
        "lessonpt.jwt.secret=01234567890123456789012345678901",
        "lessonpt.student-session.same-site=Lax"
})
@AutoConfigureMockMvc
@Import(StudentControllerSecurityTest.StudentSessionProbe.class)
class StudentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

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
    private ContentResourceMapper contentResourceMapper;

    @MockitoBean
    private StudentLearningQueryMapper studentLearningQueryMapper;

    @MockitoBean
    private TeacherStudentAccessService teacherStudentAccessService;

    @MockitoBean
    private StudentEmailVerificationService studentEmailVerificationService;

    @MockitoBean
    private StudentAccessSessionService studentAccessSessionService;

    @MockitoBean
    private StudentPortalService studentPortalService;

    @MockitoBean
    private TeacherAuthSessionMapper sessionMapper;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private StudentMapper studentMapper;

    @MockitoBean
    private LocationMapper locationMapper;

    @MockitoBean
    private TeacherStudentMapper teacherStudentMapper;

    @MockitoBean
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    private String bearerToken;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void authenticatedTeacherListsStudentsWithoutTeacherId() throws Exception {
        authenticate(21L);
        when(studentService.getStudents(21L)).thenReturn(List.of(new StudentResponse(41L, null, "김학생", null, 72L)));

        mockMvc.perform(get("/api/v1/students").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(41))
                .andExpect(jsonPath("$[0].teacherStudentId").value(72))
                .andExpect(jsonPath("$[0].teacherId").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist())
                .andExpect(jsonPath("$[0].deletedAt").doesNotExist());
    }

    @Test
    void createUsesPrincipalAndRejectsBlankName() throws Exception {
        authenticate(21L);
        when(studentService.createStudent(eq(21L), any())).thenReturn(new StudentResponse(41L, null, "김학생", null, 72L));

        mockMvc.perform(post("/api/v1/students")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"김학생"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("/api/v1/students/41")));
        verify(studentService).createStudent(eq(21L), eq(new StudentCreateRequest(null, "김학생", null)));

        mockMvc.perform(post("/api/v1/students")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
        verify(studentService, never()).createStudent(eq(21L), eq(new StudentCreateRequest(null, " ", null)));
    }

    @Test
    void otherTeacherStudentLooksLikeNotFound() throws Exception {
        authenticate(21L);
        when(studentService.getStudent(21L, 99L)).thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));

        mockMvc.perform(get("/api/v1/students/99").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"));
    }

    @Test
    void deleteReleasesThroughPrincipal() throws Exception {
        authenticate(21L);

        mockMvc.perform(delete("/api/v1/students/41").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNoContent());

        verify(studentService).releaseStudent(21L, 41L);
    }

    @Test
    void accessRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/students/41/access")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/students/41/access")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/students/41/access")).andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCreatesReadsAndRevokesAccess() throws Exception {
        authenticate(21L);
        TeacherStudentAccessResponse created = new TeacherStudentAccessResponse(
                90L, 72L, "11111111-1111-4111-8111-111111111111", null, RecordStatus.ACTIVE);
        when(teacherStudentAccessService.createAccess(21L, 41L)).thenReturn(created);
        when(teacherStudentAccessService.getAccess(21L, 41L)).thenReturn(created);

        mockMvc.perform(post("/api/v1/students/41/access").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.endsWith("/api/v1/students/41/access")))
                .andExpect(jsonPath("$.publicAccessKey").value("11111111-1111-4111-8111-111111111111"))
                .andExpect(jsonPath("$.teacherStudentId").value(72))
                .andExpect(jsonPath("$.lastVerifiedAt").doesNotExist())
                .andExpect(jsonPath("$.revokedAt").doesNotExist());

        mockMvc.perform(get("/api/v1/students/41/access").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(delete("/api/v1/students/41/access").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNoContent());
        verify(teacherStudentAccessService).revokeAccess(21L, 41L);

        when(teacherStudentAccessService.getAccess(21L, 41L)).thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        mockMvc.perform(get("/api/v1/students/41/access").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"));
    }

    @Test
    void otherTeacherAccessLooksLikeNotFound() throws Exception {
        authenticate(21L);
        when(teacherStudentAccessService.getAccess(21L, 99L)).thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));

        mockMvc.perform(get("/api/v1/students/99/access").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"));
    }

    @Test
    void otpEndpointsArePublicAndDoNotLeakLookupFailures() throws Exception {
        mockMvc.perform(post("/api/v1/student-access/missing-key/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student@lessonpt.local"}
                                """))
                .andExpect(status().isNoContent());
        verify(studentEmailVerificationService).issue("missing-key", "student@lessonpt.local");

        when(studentEmailVerificationService.verify("missing-key", "student@lessonpt.local", "123456"))
                .thenReturn(new IssuedStudentSession(
                        "raw-token",
                        java.time.LocalDateTime.parse("2026-10-24T00:00:00"),
                        java.time.LocalDateTime.parse("2027-03-23T00:00:00")));
        mockMvc.perform(post("/api/v1/student-access/missing-key/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student@lessonpt.local","otp":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.otp").doesNotExist())
                .andExpect(cookie().exists(StudentSessionProperties.COOKIE_NAME))
                .andExpect(cookie().httpOnly(StudentSessionProperties.COOKIE_NAME, true))
                .andExpect(cookie().secure(StudentSessionProperties.COOKIE_NAME, true))
                .andExpect(cookie().path(StudentSessionProperties.COOKIE_NAME, "/api/v1/student"))
                .andExpect(cookie().sameSite(StudentSessionProperties.COOKIE_NAME, "Lax"))
                .andExpect(jsonPath("$.rawToken").doesNotExist());

        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND))
                .when(studentEmailVerificationService).issue("missing-key", "student@lessonpt.local");
        mockMvc.perform(post("/api/v1/student-access/missing-key/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student@lessonpt.local"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("요청한 대상을 찾을 수 없습니다."));

        org.mockito.Mockito.doThrow(new EmailDeliveryException())
                .when(studentEmailVerificationService).issue("missing-key", "student@lessonpt.local");
        mockMvc.perform(post("/api/v1/student-access/missing-key/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student@lessonpt.local"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON_INTERNAL_ERROR"))
                .andExpect(jsonPath("$.otp").doesNotExist())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("123456"))));

        mockMvc.perform(post("/api/v1/student-access/missing-key/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":" "}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/student-access/missing-key/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student@lessonpt.local","otp":"12"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/students")).andExpect(status().isUnauthorized());
    }

    @Test
    void studentSessionCookieDoesNotAuthenticateTeacherApis() throws Exception {
        mockMvc.perform(get("/api/v1/student/session"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/student/session")
                        .cookie(new jakarta.servlet.http.Cookie(StudentSessionProperties.COOKIE_NAME, "expired")))
                .andExpect(status().isUnauthorized());
        when(studentAccessSessionService.authenticate("revoked")).thenReturn(java.util.Optional.empty());
        mockMvc.perform(get("/api/v1/student/session")
                        .cookie(new jakarta.servlet.http.Cookie(StudentSessionProperties.COOKIE_NAME, "revoked")))
                .andExpect(status().isUnauthorized());

        when(studentAccessSessionService.authenticate("raw-token"))
                .thenReturn(java.util.Optional.of(new StudentPrincipal(90L, 72L, 41L)));
        mockMvc.perform(get("/api/v1/student/session")
                        .cookie(new jakarta.servlet.http.Cookie(StudentSessionProperties.COOKIE_NAME, "raw-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherStudentAccessId").value(90))
                .andExpect(jsonPath("$.teacherStudentId").value(72))
                .andExpect(jsonPath("$.studentId").value(41));

        authenticate(8L);
        mockMvc.perform(get("/api/v1/student/session").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/students")
                        .cookie(new jakarta.servlet.http.Cookie(StudentSessionProperties.COOKIE_NAME, "raw-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesSessionAndExpiresCookie() throws Exception {
        mockMvc.perform(post("/api/v1/student/session/logout")
                        .cookie(new jakarta.servlet.http.Cookie(StudentSessionProperties.COOKIE_NAME, "raw-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
        verify(studentAccessSessionService).logout("raw-token");

        mockMvc.perform(post("/api/v1/student/session/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void studentPortalRequiresStudentSession() throws Exception {
        mockMvc.perform(get("/api/v1/student/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/student/learning")).andExpect(status().isUnauthorized());
        authenticate(8L);
        mockMvc.perform(get("/api/v1/student/learning").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isUnauthorized());

        when(studentAccessSessionService.authenticate("raw-token"))
                .thenReturn(java.util.Optional.of(new StudentPrincipal(90L, 72L, 41L)));
        when(studentPortalService.me(new StudentPrincipal(90L, 72L, 41L)))
                .thenReturn(new StudentMeResponse("학생"));
        when(studentPortalService.learning(new StudentPrincipal(90L, 72L, 41L)))
                .thenReturn(new StudentLearningResponse(List.of()));
        mockMvc.perform(get("/api/v1/student/me")
                        .cookie(new jakarta.servlet.http.Cookie(StudentSessionProperties.COOKIE_NAME, "raw-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("학생"))
                .andExpect(jsonPath("$.email").doesNotExist());
        when(studentPortalService.learning(new StudentPrincipal(null, null, 41L)))
                .thenThrow(new BusinessException(ErrorCode.STUDENT_SCOPE_REQUIRED));
        when(studentAccessSessionService.authenticate("unscoped"))
                .thenReturn(java.util.Optional.of(new StudentPrincipal(null, null, 41L)));
        mockMvc.perform(get("/api/v1/student/learning")
                        .cookie(new jakarta.servlet.http.Cookie(StudentSessionProperties.COOKIE_NAME, "unscoped")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDENT_SCOPE_REQUIRED"));
        mockMvc.perform(get("/api/v1/student/relationships")
                        .cookie(new jakarta.servlet.http.Cookie(StudentSessionProperties.COOKIE_NAME, "unscoped")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/student/session/scope"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/student/learning")
                        .cookie(new jakarta.servlet.http.Cookie(StudentSessionProperties.COOKIE_NAME, "raw-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculums").isArray())
                .andExpect(jsonPath("$.memo").doesNotExist())
                .andExpect(jsonPath("$.teacherId").doesNotExist())
                .andExpect(jsonPath("$.studentId").doesNotExist());
        mockMvc.perform(get("/api/v1/students")
                        .cookie(new jakarta.servlet.http.Cookie(StudentSessionProperties.COOKIE_NAME, "raw-token")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/students/41/learning"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/students/41/learning")
                        .cookie(new jakarta.servlet.http.Cookie(StudentSessionProperties.COOKIE_NAME, "raw-token")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/students/41/learning").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"));
    }

    @org.springframework.web.bind.annotation.RestController
    static class StudentSessionProbe {
        @org.springframework.web.bind.annotation.GetMapping("/api/v1/student/session")
        StudentPrincipal current(org.springframework.security.core.Authentication authentication) {
            return (StudentPrincipal) authentication.getPrincipal();
        }
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
