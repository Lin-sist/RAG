package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.admin.security.RequestIdentity;
import io.modelcontextprotocol.common.McpTransportContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Resolves the MCP caller identity exclusively from the authenticated server principal.
 */
public final class McpRequestIdentityResolver {

    static final String CONTEXT_KEY = "enterprise-rag.request-identity";

    private final CurrentUserService currentUserService;

    public McpRequestIdentityResolver(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public RequestIdentity resolve(HttpServletRequest request) {
        if (!(request.getUserPrincipal() instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return currentUserService.requireIdentity(null);
        }
        return currentUserService.requireIdentity(userDetails);
    }

    public RequestIdentity requireContextIdentity(McpTransportContext context) {
        Object identity = context == null ? null : context.get(CONTEXT_KEY);
        if (!(identity instanceof RequestIdentity requestIdentity)) {
            return currentUserService.requireIdentity(null);
        }
        return requestIdentity;
    }
}
