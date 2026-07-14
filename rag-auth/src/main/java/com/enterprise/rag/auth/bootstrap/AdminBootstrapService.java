package com.enterprise.rag.auth.bootstrap;

import com.enterprise.rag.auth.persistence.entity.AuthUser;
import com.enterprise.rag.auth.persistence.mapper.AuthRoleMapper;
import com.enterprise.rag.auth.persistence.mapper.AuthUserMapper;
import com.enterprise.rag.auth.persistence.mapper.AuthUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminBootstrapService {

    private static final String QUARANTINED_PASSWORD = "{c2-known-seed-quarantined}";
    private static final Set<String> KNOWN_DEFAULT_PASSWORDS = Set.of("admin123", "user123");
    private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile("^\\$\\{[^{}]+}$");

    private final AuthBootstrapProperties properties;
    private final AuthUserMapper authUserMapper;
    private final AuthRoleMapper authRoleMapper;
    private final AuthUserRoleMapper authUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public BootstrapOutcome bootstrap() {
        if (!properties.isEnabled()) {
            return BootstrapOutcome.DISABLED;
        }

        validateConfiguredValue(properties.getUsername(), "username", false);
        validateConfiguredValue(properties.getPassword(), "password", true);

        Long adminRoleId = authRoleMapper.findRoleIdByName("ADMIN");
        if (adminRoleId == null) {
            throw new IllegalStateException("Invalid auth.bootstrap state (admin-role-missing)");
        }

        AuthUser existingUser = authUserMapper.findAnyByUsername(properties.getUsername());
        if (isQuarantinedSeed(existingUser)) {
            int updated = authUserMapper.claimQuarantinedSeed(
                    existingUser.getId(),
                    passwordEncoder.encode(properties.getPassword()),
                    properties.getEmail());
            if (updated != 1) {
                throw new IllegalStateException("Invalid auth.bootstrap state (seed-claim-conflict)");
            }
            ensureRoleAssignment(existingUser.getId(), adminRoleId);
            return BootstrapOutcome.CLAIMED_QUARANTINED_SEED;
        }
        if (existingUser != null) {
            if (Integer.valueOf(0).equals(existingUser.getDeleted())
                    && authUserRoleMapper.countAssignment(existingUser.getId(), adminRoleId) > 0) {
                return BootstrapOutcome.ALREADY_BOOTSTRAPPED;
            }
            throw new IllegalStateException("Invalid auth.bootstrap state (target-not-admin)");
        }
        if (authUserMapper.countNonDeletedUsers() != 0) {
            throw new IllegalStateException("Invalid auth.bootstrap state (database-not-empty)");
        }

        LocalDateTime now = LocalDateTime.now();
        AuthUser user = new AuthUser();
        user.setUsername(properties.getUsername());
        user.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
        user.setEmail(properties.getEmail());
        user.setEnabled(true);
        user.setDeleted(0);
        user.setVersion(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        authUserMapper.insert(user);
        ensureRoleAssignment(user.getId(), adminRoleId);
        return BootstrapOutcome.CREATED;
    }

    private boolean isQuarantinedSeed(AuthUser user) {
        return user != null
                && Integer.valueOf(0).equals(user.getDeleted())
                && QUARANTINED_PASSWORD.equals(user.getPasswordHash());
    }

    private void ensureRoleAssignment(Long userId, Long roleId) {
        if (authUserRoleMapper.countAssignment(userId, roleId) == 0) {
            authUserRoleMapper.insertAssignment(userId, roleId);
        }
    }

    private void validateConfiguredValue(String value, String field, boolean rejectKnownDefault) {
        if (value == null || value.isBlank()) {
            throw invalidConfiguration(field, "blank");
        }
        if (!value.equals(value.trim())) {
            throw invalidConfiguration(field, "surrounding-whitespace");
        }
        if (UNRESOLVED_PLACEHOLDER.matcher(value).matches()) {
            throw invalidConfiguration(field, "unresolved-placeholder");
        }
        if (rejectKnownDefault && KNOWN_DEFAULT_PASSWORDS.contains(value)) {
            throw invalidConfiguration(field, "known-default");
        }
    }

    private IllegalStateException invalidConfiguration(String field, String category) {
        return new IllegalStateException(
                "Invalid auth.bootstrap." + field + " configuration (" + category + ")");
    }
}
