package com.example.authstarter.config.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTests {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void usesProvidedCorrelationIdAndClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (servletRequest, servletResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isEqualTo("trace-123");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("trace-123");
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }
}

