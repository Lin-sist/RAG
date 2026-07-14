package com.enterprise.rag.auth;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnownSeedMigrationCompatibilityTest {

    private static final String KNOWN_HASH =
            "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH";

    @Test
    void quarantinesOnlyTheKnownSeedAndPreservesItsId() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:c2_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS `user`");
                statement.execute("CREATE TABLE `user` (id BIGINT PRIMARY KEY, username VARCHAR(50), "
                        + "password_hash VARCHAR(255), enabled TINYINT, deleted TINYINT, "
                        + "version INT, updated_at TIMESTAMP)");
                statement.execute("INSERT INTO `user` VALUES (1, 'admin', '" + KNOWN_HASH
                        + "', 1, 0, 0, CURRENT_TIMESTAMP)");
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V6__quarantine_known_admin_seed.sql"));

            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT id, password_hash, enabled FROM `user` WHERE username = 'admin'")) {
                result.next();
                assertEquals(1L, result.getLong("id"));
                assertEquals("{c2-known-seed-quarantined}", result.getString("password_hash"));
                assertEquals(0, result.getInt("enabled"));
            }
        }
    }

    @Test
    void leavesChangedAdminCredentialUntouched() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:c2_changed_admin;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE `user` (id BIGINT PRIMARY KEY, username VARCHAR(50), "
                        + "password_hash VARCHAR(255), enabled TINYINT, deleted TINYINT, "
                        + "version INT, updated_at TIMESTAMP)");
                statement.execute("INSERT INTO `user` VALUES "
                        + "(9, 'admin', '$2a$10$changedHash', 1, 0, 3, CURRENT_TIMESTAMP)");
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V6__quarantine_known_admin_seed.sql"));

            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT id, password_hash, enabled, version FROM `user` WHERE username = 'admin'")) {
                result.next();
                assertEquals(9L, result.getLong("id"));
                assertEquals("$2a$10$changedHash", result.getString("password_hash"));
                assertEquals(1, result.getInt("enabled"));
                assertEquals(3, result.getInt("version"));
            }
        }
    }
}
