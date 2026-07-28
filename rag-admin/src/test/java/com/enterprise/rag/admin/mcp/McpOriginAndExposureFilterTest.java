package com.enterprise.rag.admin.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpOriginAndExposureFilterTest {

    @Test
    void wildcardOriginConfigurationIsRejected() {
        McpProperties properties = new McpProperties();
        properties.setAllowedOrigins(List.of("*"));

        assertThrows(IllegalArgumentException.class,
                () -> new McpOriginAndExposureFilter(properties));
    }

    @Test
    void unlistedOriginIsRejectedBeforeTheMcpHandler() throws Exception {
        McpProperties properties = new McpProperties();
        properties.setLocalOnly(false);
        properties.setAllowedOrigins(List.of("https://trusted.example"));
        McpOriginAndExposureFilter filter = new McpOriginAndExposureFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("Origin", "https://attacker.example");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("MCP_FORBIDDEN"));
        assertFalse(invoked.get());
    }

    @Test
    void nonLoopbackPeerIsRejectedWithoutTrustingForwardedHeaders() throws Exception {
        McpProperties properties = new McpProperties();
        properties.setLocalOnly(true);
        McpOriginAndExposureFilter filter = new McpOriginAndExposureFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("X-Forwarded-For", "127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("MCP_FORBIDDEN"));
        assertFalse(invoked.get());
    }

    @Test
    void oversizedBodyIsRejectedEvenWithoutContentLength() throws Exception {
        McpProperties properties = new McpProperties();
        properties.setLocalOnly(false);
        properties.setMaxRequestBytes(8);
        McpOriginAndExposureFilter filter = new McpOriginAndExposureFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp") {
            @Override
            public int getContentLength() {
                return -1;
            }

            @Override
            public long getContentLengthLong() {
                return -1L;
            }
        };
        request.setContent("{\"too\":\"large\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("MCP_REQUEST_TOO_LARGE"));
        assertFalse(invoked.get());
    }

    @Test
    void nonJsonPostIsRejectedBeforeProtocolParsing() throws Exception {
        McpProperties properties = new McpProperties();
        properties.setLocalOnly(false);
        McpOriginAndExposureFilter filter = new McpOriginAndExposureFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setContentType("text/plain");
        request.addHeader("Accept", "application/json, text/event-stream");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertEquals(415, response.getStatus());
        assertTrue(response.getContentAsString().contains("MCP_UNSUPPORTED_MEDIA_TYPE"));
        assertFalse(invoked.get());
    }

    @Test
    void wildcardContentTypeIsNotTreatedAsJson() throws Exception {
        McpProperties properties = new McpProperties();
        properties.setLocalOnly(false);
        McpOriginAndExposureFilter filter = new McpOriginAndExposureFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setContentType("application/*");
        request.addHeader("Accept", "application/json, text/event-stream");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertEquals(415, response.getStatus());
        assertTrue(response.getContentAsString().contains("MCP_UNSUPPORTED_MEDIA_TYPE"));
        assertFalse(invoked.get());
    }

    @Test
    void incompleteAcceptHeaderIsRejectedBeforeProtocolParsing() throws Exception {
        McpProperties properties = new McpProperties();
        properties.setLocalOnly(false);
        McpOriginAndExposureFilter filter = new McpOriginAndExposureFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setContentType("application/json");
        request.addHeader("Accept", "application/json");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertEquals(406, response.getStatus());
        assertTrue(response.getContentAsString().contains("MCP_NOT_ACCEPTABLE"));
        assertFalse(invoked.get());
    }
}
