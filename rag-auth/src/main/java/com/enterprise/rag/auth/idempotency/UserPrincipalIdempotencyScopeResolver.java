package com.enterprise.rag.auth.idempotency;

import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.common.idempotency.IdempotencyScope;
import com.enterprise.rag.common.idempotency.IdempotencyScopeResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

/**
 * 基于 Spring Security 已认证 UserPrincipal 解析幂等身份范围。
 */
@Component
public class UserPrincipalIdempotencyScopeResolver implements IdempotencyScopeResolver {

    @Override
    public Optional<IdempotencyScope> resolve(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        Principal requestPrincipal = request.getUserPrincipal();
        if (!(requestPrincipal instanceof Authentication authentication)
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            return Optional.empty();
        }

        Long tenantId = userPrincipal.getTenantId();
        Long userId = userPrincipal.getId();
        if (tenantId == null || tenantId <= 0 || userId == null || userId <= 0) {
            return Optional.empty();
        }

        return Optional.of(new IdempotencyScope(tenantId, userId));
    }
}
