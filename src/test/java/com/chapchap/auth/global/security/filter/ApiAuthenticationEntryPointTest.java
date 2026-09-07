package com.chapchap.auth.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAuthenticationEntryPointTest {
    @Test
    void returnsJsonUnauthorizedWithoutRedirectingToAnotherOrigin() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ApiAuthenticationEntryPoint(mapper).commence(
                new MockHttpServletRequest("GET", "/api/auth/users/me"), response,
                new InsufficientAuthenticationException("Authentication required"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(mapper.readTree(response.getContentAsString()).has("code")).isTrue();
    }
}
