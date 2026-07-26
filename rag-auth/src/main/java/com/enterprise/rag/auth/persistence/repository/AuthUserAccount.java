package com.enterprise.rag.auth.persistence.repository;

import java.util.Set;

public record AuthUserAccount(
        Long id,
        Long tenantId,
        String username,
        String passwordHash,
        String email,
        boolean enabled,
        Set<String> roles) {
}
