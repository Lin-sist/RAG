package com.enterprise.rag.auth.persistence.repository;

import java.util.Optional;

public interface AuthUserRepository {

    Optional<AuthUserAccount> findByUsername(String username);
}
