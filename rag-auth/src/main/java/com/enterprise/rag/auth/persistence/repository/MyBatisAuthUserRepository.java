package com.enterprise.rag.auth.persistence.repository;

import com.enterprise.rag.auth.persistence.entity.AuthUser;
import com.enterprise.rag.auth.persistence.mapper.AuthRoleMapper;
import com.enterprise.rag.auth.persistence.mapper.AuthUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class MyBatisAuthUserRepository implements AuthUserRepository {

    private final AuthUserMapper authUserMapper;
    private final AuthRoleMapper authRoleMapper;

    @Override
    public Optional<AuthUserAccount> findByUsername(String username) {
        AuthUser user = authUserMapper.findByUsername(username);
        if (user == null) {
            return Optional.empty();
        }

        List<String> roleNames = authRoleMapper.findRoleNamesByUserId(user.getId());
        Set<String> roles = roleNames == null ? Set.of() : roleNames.stream()
                .filter(role -> role != null && !role.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return Optional.of(new AuthUserAccount(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getEnabled()),
                Set.copyOf(roles)));
    }
}
