package com.enterprise.rag.auth;

import com.enterprise.rag.auth.persistence.repository.AuthUserRepository;
import com.enterprise.rag.auth.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {
        "DROP TABLE IF EXISTS user_role",
        "DROP TABLE IF EXISTS role",
        "DROP TABLE IF EXISTS `user`",
        "CREATE TABLE `user` (id BIGINT PRIMARY KEY, username VARCHAR(50) NOT NULL, "
                + "password_hash VARCHAR(255) NOT NULL, email VARCHAR(100), enabled TINYINT DEFAULT 1, "
                + "created_at TIMESTAMP, updated_at TIMESTAMP, deleted TINYINT DEFAULT 0, version INT DEFAULT 0)",
        "CREATE TABLE role (id BIGINT PRIMARY KEY, name VARCHAR(50) NOT NULL, deleted TINYINT DEFAULT 0)",
        "CREATE TABLE user_role (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, role_id BIGINT NOT NULL)",
        "INSERT INTO `user` (id, username, password_hash, email, enabled, deleted, version) "
                + "VALUES (41, 'database-admin', '$2a$10$testHash', 'admin@example.test', 1, 0, 0)",
        "INSERT INTO `user` (id, username, password_hash, email, enabled, deleted, version) "
                + "VALUES (42, 'disabled-user', '$2a$10$disabledHash', 'disabled@example.test', 0, 0, 0)",
        "INSERT INTO `user` (id, username, password_hash, email, enabled, deleted, version) "
                + "VALUES (43, 'deleted-user', '$2a$10$deletedHash', 'deleted@example.test', 1, 1, 0)",
        "INSERT INTO role (id, name, deleted) VALUES (7, 'ADMIN', 0)",
        "INSERT INTO role (id, name, deleted) VALUES (8, 'DELETED_ROLE', 1)",
        "INSERT INTO user_role (id, user_id, role_id) VALUES (11, 41, 7)",
        "INSERT INTO user_role (id, user_id, role_id) VALUES (12, 41, 8)"
})
class DatabaseUserAuthenticationTest {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loadsEnabledUserAndRolesFromDatabase() {
        UserDetails details = userDetailsService.loadUserByUsername("database-admin");

        assertEquals("database-admin", details.getUsername());
        assertTrue(details.isEnabled());
        assertEquals(Set.of("ROLE_ADMIN"), details.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet()));
    }

    @Test
    void newServiceInstanceStillLoadsPersistedDatabaseUser() {
        UserDetailsService restartedService = new UserDetailsServiceImpl(authUserRepository);

        UserDetails details = restartedService.loadUserByUsername("database-admin");

        assertEquals(41L, ((com.enterprise.rag.auth.model.UserPrincipal) details).getId());
        assertEquals("database-admin", details.getUsername());
    }

    @Test
    void exposesDisabledStateFromDatabase() {
        UserDetails details = userDetailsService.loadUserByUsername("disabled-user");

        assertEquals("disabled-user", details.getUsername());
        assertFalse(details.isEnabled());
    }

    @Test
    void doesNotLoadLogicallyDeletedUser() {
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("deleted-user"));
    }

    @Test
    void doesNotLoadMissingUser() {
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing-user"));
    }

    @Test
    void authenticatesWithDatabasePasswordHash() {
        String hash = passwordEncoder.encode("database-password");
        jdbcTemplate.update("UPDATE `user` SET password_hash = ? WHERE id = 41", hash);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("database-admin", "database-password"));

        assertEquals("database-admin", authentication.getName());
        assertTrue(authentication.isAuthenticated());
    }

    @Test
    void rejectsIncorrectDatabasePassword() {
        String hash = passwordEncoder.encode("database-password");
        jdbcTemplate.update("UPDATE `user` SET password_hash = ? WHERE id = 41", hash);

        assertThrows(BadCredentialsException.class, () -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("database-admin", "wrong-password")));
    }

    @Test
    void rejectsDisabledDatabaseUser() {
        String hash = passwordEncoder.encode("database-password");
        jdbcTemplate.update("UPDATE `user` SET password_hash = ? WHERE id = 42", hash);

        assertThrows(DisabledException.class, () -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("disabled-user", "database-password")));
    }
}
