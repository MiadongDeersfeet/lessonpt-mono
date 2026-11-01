package com.yunki.lessonpt.auth.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.FilterChain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StudentPortalAvailabilityFilterTest {
    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/student/me", "/api/v1/student/learning", "/api/v1/student-login/otp", "/api/v1/student-access/key/otp/verify", "/api/v1/students/3/access"})
    void blocksStudentTrafficBeforeAuthentication(String path) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new StudentPortalAvailabilityFilter(false).doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("STUDENT_PORTAL_PAUSED");
        verifyNoInteractions(chain);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/students", "/api/v1/students/3", "/api/v1/auth/login", "/api/v1/student/session/logout", "/api/v1/student-monitorings"})
    void preservesTeacherManagementAndLogout(String path) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new StudentPortalAvailabilityFilter(false).doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void canResumeWithoutChangingStoredData() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/student-login/otp");
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new StudentPortalAvailabilityFilter(true).doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
