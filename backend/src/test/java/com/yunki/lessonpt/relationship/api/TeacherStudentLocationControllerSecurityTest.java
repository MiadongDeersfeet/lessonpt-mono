package com.yunki.lessonpt.relationship.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.yunki.lessonpt.auth.domain.TeacherAuthSession;
import com.yunki.lessonpt.auth.jwt.IssuedToken;
import com.yunki.lessonpt.auth.jwt.JwtProvider;
import com.yunki.lessonpt.auth.mapper.TeacherAuthSessionMapper;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.dto.TeacherStudentLocationView;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.relationship.service.TeacherStudentLocationService;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = {
        "spring.profiles.active=context",
        "lessonpt.jwt.secret=01234567890123456789012345678901"
})
@AutoConfigureMockMvc
class TeacherStudentLocationControllerSecurityTest {

    private static final String BASE = "/api/v1/students/41/locations";

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
    private TeacherAuthSessionMapper sessionMapper;

    @MockitoBean
    private LocationMapper locationMapper;

    @MockitoBean
    private StudentMapper studentMapper;

    @MockitoBean
    private TeacherStudentMapper teacherStudentMapper;

    @MockitoBean
    private TeacherStudentLocationService teacherStudentLocationService;

    private String bearerToken;

    @Test
    void assignRequiresAuthentication() throws Exception {
        mockMvc.perform(post(BASE + "/30"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void assignReturnsCreatedLocationLink() throws Exception {
        authenticate(21L);
        when(teacherStudentLocationService.assignLocation(21L, 41L, 30L)).thenReturn(view());

        mockMvc.perform(post(BASE + "/30").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString(BASE + "/30")))
                .andExpect(jsonPath("$.teacherStudentLocationId").value(90))
                .andExpect(jsonPath("$.locationId").value(30))
                .andExpect(jsonPath("$.locationName").value("Main Studio"))
                .andExpect(jsonPath("$.address").value("Seoul"))
                .andExpect(jsonPath("$.teacherId").doesNotExist())
                .andExpect(jsonPath("$.teacherStudentId").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        verify(teacherStudentLocationService).assignLocation(21L, 41L, 30L);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void listReturnsAssignedLocations() throws Exception {
        authenticate(21L);
        when(teacherStudentLocationService.getStudentLocations(21L, 41L)).thenReturn(List.of(view()));

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locationName").value("Main Studio"))
                .andExpect(jsonPath("$[0].teacherStudentId").doesNotExist());
    }

    @Test
    void listReturnsEmptyArray() throws Exception {
        authenticate(21L);
        when(teacherStudentLocationService.getStudentLocations(21L, 41L)).thenReturn(List.of());

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void releaseRequiresAuthentication() throws Exception {
        mockMvc.perform(delete(BASE + "/30"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void releaseReturnsNoContent() throws Exception {
        authenticate(21L);

        mockMvc.perform(delete(BASE + "/30").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(teacherStudentLocationService).releaseLocation(21L, 41L, 30L);
    }

    @Test
    void restoreRequiresAuthentication() throws Exception {
        mockMvc.perform(post(BASE + "/30/restore"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void restoreReturnsLocationLink() throws Exception {
        authenticate(21L);
        when(teacherStudentLocationService.restoreLocation(21L, 41L, 30L)).thenReturn(view());

        mockMvc.perform(post(BASE + "/30/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherStudentLocationId").value(90))
                .andExpect(jsonPath("$.locationName").value("Main Studio"))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());
    }

    @Test
    void notFoundAndConflictUseCommonErrorResponse() throws Exception {
        authenticate(21L);
        when(teacherStudentLocationService.assignLocation(21L, 41L, 30L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));

        mockMvc.perform(post(BASE + "/30").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(BASE + "/30"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());

        when(teacherStudentLocationService.restoreLocation(21L, 41L, 30L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_CONFLICT));
        mockMvc.perform(post(BASE + "/30/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMMON_CONFLICT"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    private TeacherStudentLocationView view() {
        TeacherStudentLocationView view = new TeacherStudentLocationView();
        view.setTeacherStudentLocationId(90L);
        view.setLocationId(30L);
        view.setLocationName("Main Studio");
        view.setAddress("Seoul");
        return view;
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
