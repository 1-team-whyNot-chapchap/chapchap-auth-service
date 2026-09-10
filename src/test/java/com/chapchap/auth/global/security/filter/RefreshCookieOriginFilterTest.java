package com.chapchap.auth.global.security.filter;

import com.chapchap.auth.global.config.security.AllowedOriginProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshCookieOriginFilterTest {
    private final RefreshCookieOriginFilter filter = new RefreshCookieOriginFilter(
            new AllowedOriginProperties(List.of("https://gateway.example.test")), new ObjectMapper()
    );

    @Test
    void rejectsCrossOriginRefreshRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/reissue-token");
        request.addHeader("Origin", "https://attacker.example.test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void allowsConfiguredOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/reissue-token");
        request.addHeader("Origin", "https://gateway.example.test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }
}
