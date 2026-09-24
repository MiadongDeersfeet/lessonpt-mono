package com.yunki.lessonpt.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.yunki.lessonpt.auth.domain.TeacherAuthSession;
import com.yunki.lessonpt.auth.dto.AuthTokenResponse;
import com.yunki.lessonpt.auth.jwt.IssuedToken;
import com.yunki.lessonpt.auth.jwt.JwtProvider;
import com.yunki.lessonpt.auth.mapper.TeacherAuthSessionMapper;
import com.yunki.lessonpt.auth.service.TeacherAuthService;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.dto.TeacherSignupResponse;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;
import com.yunki.lessonpt.teacher.service.TeacherService;

@SpringBootTest(properties = {
        "spring.profiles.active=context",
        "lessonpt.jwt.secret=01234567890123456789012345678901"
})
@AutoConfigureMockMvc
class TeacherAuthSecurityTest {

    private static final String RAW_PASSWORD = "Abcdef1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private TeacherService teacherService;

    @MockitoBean
    private TeacherMapper teacherMapper;

    @MockitoBean
    private TeacherAuthSessionMapper sessionMapper;

    @MockitoBean
    private TeacherAuthService teacherAuthService;

    @MockitoBean
    private LocationMapper locationMapper;

    @MockitoBean
    private StudentMapper studentMapper;

    @MockitoBean
    private TeacherStudentMapper teacherStudentMapper;

    @Test
    void passwordEncoderBeanIsBcryptAndSignupStoresHash() {
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
        when(teacherMapper.selectTeacherByEmail("teacher@lessonpt.local")).thenReturn(null);
        AtomicReference<Teacher> stored = new AtomicReference<>();
        when(teacherMapper.insertTeacher(any())).thenAnswer(invocation -> {
            Teacher teacher = invocation.getArgument(0);
            teacher.setTeacherId(4L);
            stored.set(teacher);
            return 1;
        });
        when(teacherMapper.selectTeacherById(4L)).thenAnswer(invocation -> stored.get());

        Teacher created = teacherService.createTeacher("teacher@lessonpt.local", RAW_PASSWORD, "김강사", null);

        assertThat(created.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, created.getPasswordHash())).isTrue();
    }

    @Test
    void signupLoginAndRefreshAreNotBlockedBySecurity() throws Exception {
        when(teacherAuthService.signup(any())).thenReturn(
                new TeacherSignupResponse(4L, "teacher@lessonpt.local", "김강사", null, "TEACHER"));
        when(teacherAuthService.login(any())).thenReturn(new AuthTokenResponse("access", "refresh", 3600));
        when(teacherAuthService.refresh(any())).thenReturn(new AuthTokenResponse("access", "refresh", 3600));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"teacher@lessonpt.local","password":"Abcdef1!","name":"김강사"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"teacher@lessonpt.local","password":"Abcdef1!"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"refresh-token"}
                                """))
                .andExpect(status().isOk());
        verify(teacherAuthService).signup(any());
        verify(teacherAuthService).login(any());
        verify(teacherAuthService).refresh("refresh-token");
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meReturnsPrincipalWithoutPasswordHash() throws Exception {
        IssuedToken token = jwtProvider.createAccessToken(21L);
        Teacher teacher = new Teacher();
        teacher.setTeacherId(21L);
        teacher.setEmail("teacher@lessonpt.local");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        teacher.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        TeacherAuthSession session = new TeacherAuthSession();
        session.setAuthSessionId(3L);
        session.setTeacherId(21L);
        session.setAccessJti(token.jti());
        when(sessionMapper.selectActiveSessionByAccessJti(token.jti())).thenReturn(session);
        when(teacherMapper.selectActiveTeacherById(21L)).thenReturn(teacher);

        mockMvc.perform(get("/api/v1/teachers/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(21))
                .andExpect(jsonPath("$.email").value("teacher@lessonpt.local"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
