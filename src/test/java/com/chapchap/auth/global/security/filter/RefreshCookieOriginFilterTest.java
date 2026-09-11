package com.chapchap.auth.global.security.filter;

import com.chapchap.auth.global.config.security.AllowedOriginProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshCookieOriginFilterTest {
    @Test
    void applicationConfigAllowsFrontendAndRejectsOtherOrigins() throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("GATEWAY_URI", "http://localhost:8080")
                .withProperty("FRONTEND_ORIGIN", "http://localhost:5173");
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml"))
                .forEach(source -> environment.getPropertySources().addLast(source));
        AllowedOriginProperties origins = new AllowedOriginProperties(List.of(
                environment.getRequiredProperty("app.security.allowed-origins[0]"),
                environment.getRequiredProperty("app.security.allowed-origins[1]")
        ));
        RefreshCookieOriginFilter configured = new RefreshCookieOriginFilter(origins, new ObjectMapper());
        for (String path : List.of("/api/auth/reissue-token", "/api/auth/logout")) {
            for (String origin : List.of("http://localhost:5173", "http://localhost:8080",
                    "http://localhost:5174", "https://attacker.example.test", "http://localhost:5173.evil.test")) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
                request.addHeader("Origin", origin);
                MockHttpServletResponse response = new MockHttpServletResponse();
                MockFilterChain chain = new MockFilterChain();
                configured.doFilter(request, response, chain);
                if (origin.equals("http://localhost:5173") || origin.equals("http://localhost:8080")) {
                    assertThat(chain.getRequest()).isSameAs(request);
                } else {
                    assertThat(response.getStatus()).isEqualTo(403);
                    assertThat(chain.getRequest()).isNull();
                }
            }
        }
    }

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
