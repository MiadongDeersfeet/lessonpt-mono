package com.yunki.lessonpt.student.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.dto.StudentCreateRequest;
import com.yunki.lessonpt.student.dto.StudentResponse;
import com.yunki.lessonpt.student.service.StudentService;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = {
        "spring.profiles.active=context",
        "lessonpt.jwt.secret=01234567890123456789012345678901"
})
@AutoConfigureMockMvc
class StudentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private TeacherMapper teacherMapper;

    @MockitoBean
    private TeacherAuthSessionMapper sessionMapper;

    @MockitoBean
    private StudentService studentService;

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
