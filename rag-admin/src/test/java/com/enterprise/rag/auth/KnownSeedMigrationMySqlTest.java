package com.enterprise.rag.auth;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class KnownSeedMigrationMySqlTest {

    private static final String KNOWN_HASH =
            "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("rag_c2_migration")
            .withUsername("rag_test")
            .withPassword("rag_test_password");

    @Test
    void freshDatabaseQuarantinesKnownSeedAndRepeatMigrateIsIdempotent() throws Exception {
        Flyway flyway = flyway(null);

        flyway.clean();
        flyway.migrate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT id, password_hash, enabled FROM `user` WHERE username = 'admin'")) {
            assertTrue(result.next());
            assertEquals(1L, result.getLong("id"));
            assertEquals("{c2-known-seed-quarantined}", result.getString("password_hash"));
            assertEquals(0, result.getInt("enabled"));
        }

        assertEquals(0, flyway.migrate().migrationsExecuted);
        flyway.validate();
    }

    @Test
    void v5DatabaseQuarantinesExactKnownSeedWithoutChangingItsId() throws Exception {
        Flyway v5Flyway = flyway(MigrationVersion.fromVersion("5"));
        v5Flyway.clean();
        v5Flyway.migrate();

        long userId;
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT id, password_hash, enabled FROM `user` WHERE username = 'admin'")) {
            assertTrue(result.next());
            userId = result.getLong("id");
            assertEquals(KNOWN_HASH, result.getString("password_hash"));
            assertEquals(1, result.getInt("enabled"));
        }

        Flyway latestFlyway = flyway(null);
        latestFlyway.migrate();
        latestFlyway.validate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT id, password_hash, enabled FROM `user` WHERE username = 'admin'")) {
            assertTrue(result.next());
            assertEquals(userId, result.getLong("id"));
            assertEquals("{c2-known-seed-quarantined}", result.getString("password_hash"));
            assertEquals(0, result.getInt("enabled"));
        }
    }

    @Test
    void v5DatabaseWithChangedAdminCredentialRemainsUntouched() throws Exception {
        Flyway v5Flyway = flyway(MigrationVersion.fromVersion("5"));
        v5Flyway.clean();
        v5Flyway.migrate();

        long userId;
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery(
                    "SELECT id, password_hash FROM `user` WHERE username = 'admin'")) {
                assertTrue(result.next());
                userId = result.getLong("id");
                assertEquals(KNOWN_HASH, result.getString("password_hash"));
            }
            statement.executeUpdate(
                    "UPDATE `user` SET password_hash = '$2a$10$changedHash', enabled = 1, version = 3 "
                            + "WHERE username = 'admin'");
        }

        Flyway latestFlyway = flyway(null);
        latestFlyway.migrate();
        latestFlyway.validate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT id, password_hash, enabled, version FROM `user` WHERE username = 'admin'")) {
            assertTrue(result.next());
            assertEquals(userId, result.getLong("id"));
            assertEquals("$2a$10$changedHash", result.getString("password_hash"));
            assertEquals(1, result.getInt("enabled"));
            assertEquals(3, result.getInt("version"));
        }
    }

    @Test
    void v6DocumentRowsRemainCompatibleAfterDurableInputMigration() throws Exception {
        Flyway v6Flyway = flyway(MigrationVersion.fromVersion("6"));
        v6Flyway.clean();
        v6Flyway.migrate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO document (kb_id, uploader_id, title, file_path, file_type, status) "
                            + "VALUES (10, 20, 'legacy', NULL, 'md', 'COMPLETED')");
        }

        Flyway latestFlyway = flyway(null);
        latestFlyway.migrate();
        latestFlyway.validate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT input_size_bytes, input_sha256, input_state FROM document WHERE title = 'legacy'")) {
            assertTrue(result.next());
            assertEquals(null, result.getObject("input_size_bytes"));
            assertEquals(null, result.getString("input_sha256"));
            assertEquals(null, result.getString("input_state"));
        }
    }

    private static Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }
}
