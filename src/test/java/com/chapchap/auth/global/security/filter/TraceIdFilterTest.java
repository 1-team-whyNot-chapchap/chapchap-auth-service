package com.chapchap.auth.global.security.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
class TraceIdFilterTest {

    @Test
    void keepsValidTraceIdInResponseAndClearsMdcAfterRequest() throws Exception {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> assertThat(MDC.get("traceId")).isEqualTo("trace-123");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace-123");
        assertThat(MDC.get("traceId")).isNull();
    }
}
