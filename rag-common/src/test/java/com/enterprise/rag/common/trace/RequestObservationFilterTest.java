package com.enterprise.rag.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RequestObservationFilterTest {

    @AfterEach
    void cleanup() {
        TraceContext.clearAll();
    }

    @Test
    void shouldPassThroughOnNormalRequest() throws ServletException, IOException {
        RequestObservationFilter filter = new RequestObservationFilter(1);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/qa/ask");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        TraceContext.setTraceId("trace-test-1");

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldRethrowWhenChainFails() throws ServletException, IOException {
        RequestObservationFilter filter = new RequestObservationFilter(1000);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/history/1/feedback");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        doThrow(new ServletException("boom")).when(chain).doFilter(request, response);

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));
        verify(chain, times(1)).doFilter(request, response);
    }
}
