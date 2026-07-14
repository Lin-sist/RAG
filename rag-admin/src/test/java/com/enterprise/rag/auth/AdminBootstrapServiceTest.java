package com.enterprise.rag.auth;

import com.enterprise.rag.auth.bootstrap.AdminBootstrapService;
import com.enterprise.rag.auth.bootstrap.AuthBootstrapProperties;
import com.enterprise.rag.auth.bootstrap.BootstrapOutcome;
import com.enterprise.rag.auth.model.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

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
        "CREATE TABLE `user` (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, "
                + "password_hash VARCHAR(255) NOT NULL, email VARCHAR(100), enabled TINYINT DEFAULT 1, "
                + "created_at TIMESTAMP, updated_at TIMESTAMP, deleted TINYINT DEFAULT 0, version INT DEFAULT 0)",
        "CREATE TABLE role (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL UNIQUE, "
                + "deleted TINYINT DEFAULT 0)",
        "CREATE TABLE user_role (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, "
                + "role_id BIGINT NOT NULL, UNIQUE(user_id, role_id))",
        "INSERT INTO role (id, name, deleted) VALUES (7, 'ADMIN', 0)"
})
class AdminBootstrapServiceTest {

    @Autowired
    private AdminBootstrapService adminBootstrapService;

    @Autowired
    private AuthBootstrapProperties properties;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetProperties() {
        properties.setEnabled(false);
        properties.setUsername(null);
        properties.setPassword(null);
        properties.setEmail(null);
    }

    @Test
    void staysDisabledUnlessExplicitlyEnabled() {
        assertEquals(BootstrapOutcome.DISABLED, adminBootstrapService.bootstrap());
    }

    @Test
    void createsFirstAdminFromExternalCredentials() {
        properties.setEnabled(true);
        properties.setUsername("first-admin");
        properties.setPassword("external-bootstrap-password");
        properties.setEmail("first-admin@example.test");

        assertEquals(BootstrapOutcome.CREATED, adminBootstrapService.bootstrap());

        UserDetails details = userDetailsService.loadUserByUsername("first-admin");
        assertTrue(details.isEnabled());
        assertTrue(passwordEncoder.matches("external-bootstrap-password", details.getPassword()));
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void claimsQuarantinedSeedWithoutChangingItsId() {
        jdbcTemplate.update("INSERT INTO `user` "
                + "(id, username, password_hash, email, enabled, deleted, version) VALUES (?, ?, ?, ?, ?, ?, ?)",
                99L, "admin", "{c2-known-seed-quarantined}", "old@example.test", 0, 0, 1);
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("external-bootstrap-password");
        properties.setEmail("new@example.test");

        assertEquals(BootstrapOutcome.CLAIMED_QUARANTINED_SEED, adminBootstrapService.bootstrap());

        UserPrincipal details = (UserPrincipal) userDetailsService.loadUserByUsername("admin");
        assertEquals(99L, details.getId());
        assertTrue(details.isEnabled());
        assertTrue(passwordEncoder.matches("external-bootstrap-password", details.getPassword()));
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void leavesExistingAdminUntouchedOnRepeatedBootstrap() {
        String existingHash = passwordEncoder.encode("existing-password");
        jdbcTemplate.update("INSERT INTO `user` "
                + "(id, username, password_hash, email, enabled, deleted, version) VALUES (?, ?, ?, ?, ?, ?, ?)",
                77L, "existing-admin", existingHash, "existing@example.test", 1, 0, 4);
        jdbcTemplate.update("INSERT INTO user_role (user_id, role_id) VALUES (?, ?)", 77L, 7L);
        properties.setEnabled(true);
        properties.setUsername("existing-admin");
        properties.setPassword("replacement-password");

        assertEquals(BootstrapOutcome.ALREADY_BOOTSTRAPPED, adminBootstrapService.bootstrap());

        UserDetails details = userDetailsService.loadUserByUsername("existing-admin");
        assertTrue(passwordEncoder.matches("existing-password", details.getPassword()));
    }

    @Test
    void rejectsKnownDefaultPasswordWithoutEchoingIt() {
        properties.setEnabled(true);
        properties.setUsername("first-admin");
        properties.setPassword("admin123");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> adminBootstrapService.bootstrap());

        assertFalse(error.getMessage().contains("admin123"));
    }

    @Test
    void rejectsMissingAdminRoleWithoutCreatingPartialUser() {
        jdbcTemplate.update("DELETE FROM role WHERE name = 'ADMIN'");
        properties.setEnabled(true);
        properties.setUsername("first-admin");
        properties.setPassword("external-bootstrap-password");

        assertThrows(IllegalStateException.class, () -> adminBootstrapService.bootstrap());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class));
    }

    @Test
    void rollsBackCreatedUserWhenRoleAssignmentFails() {
        jdbcTemplate.execute("DROP TABLE user_role");
        properties.setEnabled(true);
        properties.setUsername("first-admin");
        properties.setPassword("external-bootstrap-password");

        assertThrows(RuntimeException.class, () -> adminBootstrapService.bootstrap());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", " username-with-whitespace ", "${AUTH_BOOTSTRAP_USERNAME}"})
    void rejectsInvalidExternalUsernameWithoutEchoingIt(String candidate) {
        properties.setEnabled(true);
        properties.setUsername(candidate);
        properties.setPassword("external-bootstrap-password");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> adminBootstrapService.bootstrap());

        if (!candidate.isBlank()) {
            assertFalse(error.getMessage().contains(candidate));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", " password-with-whitespace ", "${AUTH_BOOTSTRAP_PASSWORD}"})
    void rejectsInvalidExternalPasswordWithoutEchoingIt(String candidate) {
        properties.setEnabled(true);
        properties.setUsername("first-admin");
        properties.setPassword(candidate);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> adminBootstrapService.bootstrap());

        if (!candidate.isBlank()) {
            assertFalse(error.getMessage().contains(candidate));
        }
    }

    @Test
    void refusesToElevateExistingNonAdminUser() {
        String existingHash = passwordEncoder.encode("existing-password");
        jdbcTemplate.update("INSERT INTO `user` "
                + "(id, username, password_hash, email, enabled, deleted, version) VALUES (?, ?, ?, ?, ?, ?, ?)",
                78L, "existing-user", existingHash, "user@example.test", 1, 0, 0);
        properties.setEnabled(true);
        properties.setUsername("existing-user");
        properties.setPassword("external-bootstrap-password");

        assertThrows(IllegalStateException.class, () -> adminBootstrapService.bootstrap());

        UserDetails details = userDetailsService.loadUserByUsername("existing-user");
        assertTrue(passwordEncoder.matches("existing-password", details.getPassword()));
        assertTrue(details.getAuthorities().isEmpty());
    }

    @Test
    void refusesToInjectAdminIntoNonEmptyDatabase() {
        jdbcTemplate.update("INSERT INTO `user` "
                + "(id, username, password_hash, email, enabled, deleted, version) VALUES (?, ?, ?, ?, ?, ?, ?)",
                79L, "someone-else", passwordEncoder.encode("existing-password"),
                "someone@example.test", 1, 0, 0);
        properties.setEnabled(true);
        properties.setUsername("new-admin");
        properties.setPassword("external-bootstrap-password");

        assertThrows(IllegalStateException.class, () -> adminBootstrapService.bootstrap());
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("new-admin"));
    }
}
