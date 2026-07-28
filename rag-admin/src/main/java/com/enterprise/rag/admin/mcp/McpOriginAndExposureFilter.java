package com.enterprise.rag.admin.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class McpOriginAndExposureFilter extends OncePerRequestFilter {

    private static final String FORBIDDEN_RESPONSE =
            "{\"errorCode\":\"MCP_FORBIDDEN\",\"message\":\"MCP request rejected\"}";
    private static final String REQUEST_TOO_LARGE_RESPONSE =
            "{\"errorCode\":\"MCP_REQUEST_TOO_LARGE\","
                    + "\"message\":\"MCP request exceeds the configured limit\"}";
    private static final String UNSUPPORTED_MEDIA_TYPE_RESPONSE =
            "{\"errorCode\":\"MCP_UNSUPPORTED_MEDIA_TYPE\","
                    + "\"message\":\"MCP POST requires application/json\"}";
    private static final String NOT_ACCEPTABLE_RESPONSE =
            "{\"errorCode\":\"MCP_NOT_ACCEPTABLE\","
                    + "\"message\":\"MCP POST requires JSON and SSE response support\"}";

    private final Set<String> allowedOrigins;
    private final boolean localOnly;
    private final int maxRequestBytes;

    public McpOriginAndExposureFilter(McpProperties properties) {
        this.allowedOrigins = Set.copyOf(properties.getAllowedOrigins());
        this.localOnly = properties.isLocalOnly();
        this.maxRequestBytes = properties.getMaxRequestBytes();
        if (allowedOrigins.contains("*")) {
            throw new IllegalArgumentException("MCP allowed origins must not contain a wildcard");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestPath = request.getRequestURI().substring(contextPath.length());
        return !McpServerConfiguration.ENDPOINT.equals(requestPath);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin) && !allowedOrigins.contains(origin)) {
            reject(response);
            return;
        }
        if (localOnly && !isLoopback(request.getRemoteAddr())) {
            reject(response);
            return;
        }

        long declaredLength = request.getContentLengthLong();
        if (declaredLength > maxRequestBytes) {
            rejectTooLarge(response);
            return;
        }

        byte[] body = request.getInputStream().readNBytes(maxRequestBytes + 1);
        if (body.length > maxRequestBytes) {
            rejectTooLarge(response);
            return;
        }
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            if (!hasJsonContentType(request)) {
                rejectMediaType(response);
                return;
            }
            if (!acceptsJsonAndEventStream(request)) {
                rejectNotAcceptable(response);
                return;
            }
        }
        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private boolean hasJsonContentType(HttpServletRequest request) {
        try {
            MediaType contentType = MediaType.parseMediaType(request.getContentType());
            return isExplicitJson(contentType);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean acceptsJsonAndEventStream(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        if (!StringUtils.hasText(acceptHeader)) {
            return false;
        }
        try {
            var accepted = MediaType.parseMediaTypes(acceptHeader);
            boolean acceptsJson = accepted.stream().anyMatch(this::isExplicitJson);
            boolean acceptsEventStream = accepted.stream().anyMatch(this::isExplicitEventStream);
            return acceptsJson && acceptsEventStream;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isExplicitJson(MediaType mediaType) {
        return "application".equalsIgnoreCase(mediaType.getType())
                && "json".equalsIgnoreCase(mediaType.getSubtype());
    }

    private boolean isExplicitEventStream(MediaType mediaType) {
        return "text".equalsIgnoreCase(mediaType.getType())
                && "event-stream".equalsIgnoreCase(mediaType.getSubtype());
    }

    private boolean isLoopback(String remoteAddress) {
        if (!StringUtils.hasText(remoteAddress)
                || !remoteAddress.matches("[0-9a-fA-F:.]+")) {
            return false;
        }
        try {
            return InetAddress.getByName(remoteAddress).isLoopbackAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(FORBIDDEN_RESPONSE);
    }

    private void rejectTooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(REQUEST_TOO_LARGE_RESPONSE);
    }

    private void rejectMediaType(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(UNSUPPORTED_MEDIA_TYPE_RESPONSE);
    }

    private void rejectNotAcceptable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(NOT_ACCEPTABLE_RESPONSE);
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // The MCP servlet consumes the already-buffered request synchronously.
                }

                @Override
                public int read() {
                    return input.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = StringUtils.hasText(encoding)
                    ? Charset.forName(encoding)
                    : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
