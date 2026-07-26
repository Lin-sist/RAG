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
class TenantModelMigrationMySqlTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("rag_c13a_migration")
            .withUsername("rag_test")
            .withPassword("rag_test_password");

    @Test
    void v9UpgradeBackfillsOneLegacyTenantWithoutChangingExistingRelationships() throws Exception {
        Flyway v9Flyway = flyway(MigrationVersion.fromVersion("9"));
        v9Flyway.clean();
        v9Flyway.migrate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO `user`
                        (id, username, password_hash, email, enabled, deleted, version)
                    VALUES
                        (41, 'disabled-user', '$2a$10$disabledHash', 'disabled@example.test', 0, 0, 3),
                        (42, 'deleted-user', '$2a$10$deletedHash', 'deleted@example.test', 1, 1, 4)
                    """);
            statement.executeUpdate("""
                    INSERT INTO knowledge_base
                        (id, name, owner_id, document_count, is_public, deleted, version)
                    VALUES
                        (71, 'private-kb', 41, 0, 0, 0, 5),
                        (72, 'deleted-public-kb', 42, 0, 1, 1, 6)
                    """);
            statement.executeUpdate("""
                    INSERT INTO kb_permission
                        (id, kb_id, user_id, permission_type, deleted, version)
                    VALUES (81, 71, 41, 'READ', 0, 7)
                    """);
        }

        Flyway latestFlyway = flyway(null);
        latestFlyway.migrate();
        latestFlyway.validate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            long tenantId;
            try (ResultSet tenant = statement.executeQuery(
                    "SELECT id, code, enabled FROM tenant WHERE code = 'legacy-default'")) {
                assertTrue(tenant.next());
                tenantId = tenant.getLong("id");
                assertTrue(tenantId > 0);
                assertEquals("legacy-default", tenant.getString("code"));
                assertEquals(1, tenant.getInt("enabled"));
            }

            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM tenant WHERE code = 'legacy-default'"));
            assertEquals(0, scalar(statement,
                    "SELECT COUNT(*) FROM `user` WHERE tenant_id IS NULL"));
            assertEquals(0, scalar(statement,
                    "SELECT COUNT(*) FROM knowledge_base WHERE tenant_id IS NULL"));

            try (ResultSet user = statement.executeQuery(
                    "SELECT id, tenant_id, enabled, deleted, version FROM `user` WHERE id = 41")) {
                assertTrue(user.next());
                assertEquals(41L, user.getLong("id"));
                assertEquals(tenantId, user.getLong("tenant_id"));
                assertEquals(0, user.getInt("enabled"));
                assertEquals(0, user.getInt("deleted"));
                assertEquals(3, user.getInt("version"));
            }

            try (ResultSet knowledgeBase = statement.executeQuery(
                    "SELECT id, tenant_id, owner_id, is_public, deleted, version "
                            + "FROM knowledge_base WHERE id = 72")) {
                assertTrue(knowledgeBase.next());
                assertEquals(72L, knowledgeBase.getLong("id"));
                assertEquals(tenantId, knowledgeBase.getLong("tenant_id"));
                assertEquals(42L, knowledgeBase.getLong("owner_id"));
                assertEquals(1, knowledgeBase.getInt("is_public"));
                assertEquals(1, knowledgeBase.getInt("deleted"));
                assertEquals(6, knowledgeBase.getInt("version"));
            }

            try (ResultSet permission = statement.executeQuery(
                    "SELECT id, kb_id, user_id, permission_type, deleted, version "
                            + "FROM kb_permission WHERE id = 81")) {
                assertTrue(permission.next());
                assertEquals(81L, permission.getLong("id"));
                assertEquals(71L, permission.getLong("kb_id"));
                assertEquals(41L, permission.getLong("user_id"));
                assertEquals("READ", permission.getString("permission_type"));
                assertEquals(0, permission.getInt("deleted"));
                assertEquals(7, permission.getInt("version"));
            }
        }
    }

    @Test
    void freshInstallCreatesNonNullTenantBoundariesAndRepeatMigrateIsIdempotent() throws Exception {
        Flyway flyway = flyway(null);
        flyway.clean();
        flyway.migrate();
        flyway.validate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM tenant WHERE code = 'legacy-default' "
                            + "AND enabled = 1 AND deleted = 0"));
            assertEquals(0, scalar(statement,
                    "SELECT COUNT(*) FROM `user` WHERE tenant_id IS NULL"));
            assertEquals(0, scalar(statement,
                    "SELECT COUNT(*) FROM knowledge_base WHERE tenant_id IS NULL"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = 'user' "
                            + "AND column_name = 'tenant_id' AND is_nullable = 'NO'"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = 'knowledge_base' "
                            + "AND column_name = 'tenant_id' AND is_nullable = 'NO'"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM information_schema.statistics "
                            + "WHERE table_schema = DATABASE() AND table_name = 'user' "
                            + "AND index_name = 'idx_user_tenant_id'"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM information_schema.statistics "
                            + "WHERE table_schema = DATABASE() AND table_name = 'knowledge_base' "
                            + "AND index_name = 'idx_kb_tenant_id'"));
        }

        assertEquals(0, flyway.migrate().migrationsExecuted);
        flyway.validate();
    }

    private static long scalar(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
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
