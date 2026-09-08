package com.enterprise.rag.auth;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class TenantDataPlaneMigrationMySqlTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("rag_c13b_migration")
            .withUsername("rag_test")
            .withPassword("rag_test_password");

    @Test
    void v10UpgradeBackfillsTwoTenantsAndPreservesBusinessFacts() throws Exception {
        Flyway v10 = flyway(MigrationVersion.fromVersion("10"));
        v10.clean();
        v10.migrate();

        long tenantId;
        long userId;
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            tenantId = scalar(statement, "SELECT id FROM tenant WHERE code = 'legacy-default'");
            userId = scalar(statement, "SELECT id FROM `user` WHERE username = 'admin'");
            statement.executeUpdate("""
                    INSERT INTO knowledge_base
                        (id, tenant_id, name, owner_id, vector_collection, document_count, is_public)
                    VALUES (1301, %d, 'c13b-kb', %d, 'kb_c13b_1301', 1, 0)
                    """.formatted(tenantId, userId));
            statement.executeUpdate("""
                    INSERT INTO document
                        (id, kb_id, uploader_id, title, file_type, status, chunk_count)
                    VALUES (1302, 1301, %d, 'c13b-doc', 'md', 'COMPLETED', 1)
                    """.formatted(userId));
            statement.executeUpdate("""
                    INSERT INTO document_chunk
                        (id, document_id, vector_id, content, chunk_index)
                    VALUES (1303, 1302, 'vector-c13b-1303', 'tenant content', 0)
                    """);
            statement.executeUpdate("""
                    INSERT INTO kb_permission
                        (id, kb_id, user_id, permission_type)
                    VALUES (1304, 1301, %d, 'READ')
                    """.formatted(userId));
            statement.executeUpdate("""
                    INSERT INTO qa_history
                        (id, user_id, kb_id, question, answer)
                    VALUES (1305, %d, 1301, 'question', 'answer')
                    """.formatted(userId));
            statement.executeUpdate("""
                    INSERT INTO qa_feedback
                        (id, qa_id, user_id, rating, comment)
                    VALUES (1306, 1305, %d, 5, 'useful')
                    """.formatted(userId));
            statement.executeUpdate("""
                    INSERT INTO async_task
                        (id, task_id, task_type, document_id, owner_id, status, execution_phase)
                    VALUES (1307, 'task-c13b-1307', 'DOCUMENT_INDEX', 1302, %d,
                            'RUNNING', 'SAFE_PRE_VECTOR')
                    """.formatted(userId));
            statement.executeUpdate("""
                    INSERT INTO tenant (id, code, name, enabled, deleted, version)
                    VALUES (2300, 'tenant-happy-b', 'Tenant Happy B', 1, 0, 2)
                    """);
            statement.executeUpdate("""
                    INSERT INTO `user`
                        (id, tenant_id, username, password_hash, enabled, deleted, version)
                    VALUES (2301, 2300, 'tenant-happy-b-user', 'test-only', 1, 0, 3)
                    """);
            statement.executeUpdate("""
                    INSERT INTO knowledge_base
                        (id, tenant_id, name, owner_id, vector_collection, document_count,
                         is_public, deleted, version)
                    VALUES (2302, 2300, 'tenant-b-deleted-kb', 2301, 'kb_c13b_2302', 1,
                            1, 1, 6)
                    """);
            statement.executeUpdate("""
                    INSERT INTO document
                        (id, kb_id, uploader_id, title, file_path, input_size_bytes,
                         input_sha256, input_state, file_type, content_hash, status,
                         chunk_count, deleted, version)
                    VALUES (2303, 2302, 2301, 'tenant-b-deleted-doc', 'objects/tenant-b', 17,
                            REPEAT('c', 64), 'AVAILABLE', 'md', REPEAT('d', 64), 'FAILED',
                            1, 1, 7)
                    """);
            statement.executeUpdate("""
                    INSERT INTO document_chunk
                        (id, document_id, vector_id, content, chunk_index, start_pos, end_pos,
                         metadata, deleted, version)
                    VALUES (2304, 2303, 'vector-c13b-2304', 'deleted tenant content', 0, 0, 22,
                            JSON_OBJECT('source', 'tenant-b'), 1, 8)
                    """);
            statement.executeUpdate("""
                    INSERT INTO kb_permission
                        (id, kb_id, user_id, permission_type, deleted, version)
                    VALUES (2305, 2302, 2301, 'ADMIN', 1, 9)
                    """);
            statement.executeUpdate("""
                    INSERT INTO qa_history
                        (id, user_id, kb_id, question, answer, trace_id, latency_ms,
                         deleted, version)
                    VALUES (2306, 2301, 2302, 'tenant b question', 'tenant b answer',
                            'trace-c13b-2306', 321, 1, 10)
                    """);
            statement.executeUpdate("""
                    INSERT INTO qa_feedback
                        (id, qa_id, user_id, rating, comment, deleted, version)
                    VALUES (2307, 2306, 2301, 1, 'tenant b feedback', 1, 11)
                    """);
            statement.executeUpdate("""
                    INSERT INTO async_task
                        (id, task_id, task_type, document_id, owner_id, status, progress,
                         execution_phase, attempt_count, lease_owner, lease_until,
                         deleted, version)
                    VALUES (2308, 'task-c13b-2308', 'DOCUMENT_INDEX', 2303, 2301, 'RUNNING', 61,
                            'VECTOR_IN_FLIGHT', 2, 'worker-b', '2026-07-26 12:00:00.123456',
                            1, 12)
                    """);
        }

        Flyway latest = flyway(null);
        latest.migrate();
        latest.validate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            assertTenant(statement, "document", "id", 1302, tenantId);
            assertTenant(statement, "document_chunk", "id", 1303, tenantId);
            assertTenant(statement, "kb_permission", "id", 1304, tenantId);
            assertTenant(statement, "qa_history", "id", 1305, tenantId);
            assertTenant(statement, "qa_feedback", "id", 1306, tenantId);
            assertTenant(statement, "async_task", "id", 1307, tenantId);
            assertTenant(statement, "document", "id", 2303, 2300);
            assertTenant(statement, "document_chunk", "id", 2304, 2300);
            assertTenant(statement, "kb_permission", "id", 2305, 2300);
            assertTenant(statement, "qa_history", "id", 2306, 2300);
            assertTenant(statement, "qa_feedback", "id", 2307, 2300);
            assertTenant(statement, "async_task", "id", 2308, 2300);

            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM knowledge_base WHERE id = 2302 AND tenant_id = 2300 "
                            + "AND owner_id = 2301 AND document_count = 1 AND is_public = 1 "
                            + "AND deleted = 1 AND version = 6"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM document WHERE id = 2303 AND tenant_id = 2300 "
                            + "AND kb_id = 2302 AND uploader_id = 2301 AND status = 'FAILED' "
                            + "AND chunk_count = 1 AND input_size_bytes = 17 "
                            + "AND input_state = 'AVAILABLE' AND deleted = 1 AND version = 7"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM document_chunk WHERE id = 2304 AND tenant_id = 2300 "
                            + "AND document_id = 2303 AND vector_id = 'vector-c13b-2304' "
                            + "AND chunk_index = 0 AND deleted = 1 AND version = 8"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM kb_permission WHERE id = 2305 AND tenant_id = 2300 "
                            + "AND kb_id = 2302 AND user_id = 2301 AND permission_type = 'ADMIN' "
                            + "AND deleted = 1 AND version = 9"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM qa_history WHERE id = 2306 AND tenant_id = 2300 "
                            + "AND user_id = 2301 AND kb_id = 2302 AND trace_id = 'trace-c13b-2306' "
                            + "AND latency_ms = 321 AND deleted = 1 AND version = 10"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM qa_feedback WHERE id = 2307 AND tenant_id = 2300 "
                            + "AND qa_id = 2306 AND user_id = 2301 AND rating = 1 "
                            + "AND comment = 'tenant b feedback' AND deleted = 1 AND version = 11"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM async_task WHERE id = 2308 AND tenant_id = 2300 "
                            + "AND task_id = 'task-c13b-2308' AND document_id = 2303 "
                            + "AND owner_id = 2301 AND status = 'RUNNING' AND progress = 61 "
                            + "AND execution_phase = 'VECTOR_IN_FLIGHT' AND attempt_count = 2 "
                            + "AND lease_owner = 'worker-b' AND deleted = 1 AND version = 12"));

            for (String table : new String[]{
                    "document", "document_chunk", "kb_permission",
                    "qa_history", "qa_feedback", "async_task"}) {
                assertEquals(1, scalar(statement,
                        "SELECT COUNT(*) FROM information_schema.columns "
                                + "WHERE table_schema = DATABASE() AND table_name = '" + table + "' "
                                + "AND column_name = 'tenant_id' AND is_nullable = 'NO'"));
            }
        }
    }

    @Test
    void v10UpgradeDerivesNullableKbHistoryAndFeedbackFromUserTenant() throws Exception {
        Flyway v10 = flyway(MigrationVersion.fromVersion("10"));
        v10.clean();
        v10.migrate();

        long tenantId;
        long userId;
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            tenantId = scalar(statement, "SELECT id FROM tenant WHERE code = 'legacy-default'");
            userId = scalar(statement, "SELECT id FROM `user` WHERE username = 'admin'");
            statement.executeUpdate("""
                    INSERT INTO qa_history
                        (id, user_id, kb_id, question, answer, trace_id, deleted, version)
                    VALUES (2600, %d, NULL, 'history without kb', 'answer without kb',
                            'trace-c13b-2600', 1, 4)
                    """.formatted(userId));
            statement.executeUpdate("""
                    INSERT INTO qa_feedback
                        (id, qa_id, user_id, rating, comment, deleted, version)
                    VALUES (2601, 2600, %d, 4, 'feedback without kb', 1, 5)
                    """.formatted(userId));
        }

        Flyway latest = flyway(null);
        latest.migrate();
        latest.validate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            assertTenant(statement, "qa_history", "id", 2600, tenantId);
            assertTenant(statement, "qa_feedback", "id", 2601, tenantId);
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM qa_history WHERE id = 2600 AND kb_id IS NULL "
                            + "AND question = 'history without kb' AND trace_id = 'trace-c13b-2600' "
                            + "AND deleted = 1 AND version = 4"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM qa_feedback WHERE id = 2601 AND qa_id = 2600 "
                            + "AND comment = 'feedback without kb' AND deleted = 1 AND version = 5"));
        }
    }

    @Test
    void v10UpgradeDerivesOwnerOnlyTaskFromOwnerTenant() throws Exception {
        Flyway v10 = flyway(MigrationVersion.fromVersion("10"));
        v10.clean();
        v10.migrate();

        long tenantId;
        long userId;
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            tenantId = scalar(statement, "SELECT id FROM tenant WHERE code = 'legacy-default'");
            userId = scalar(statement, "SELECT id FROM `user` WHERE username = 'admin'");
            statement.executeUpdate("""
                    INSERT INTO async_task
                        (id, task_id, task_type, document_id, owner_id, status, progress,
                         execution_phase, attempt_count, failure_code, deleted, version)
                    VALUES (2700, 'task-c13b-owner-only', 'LEGACY_GENERIC', NULL, %d,
                            'FAILED', 73, 'TERMINAL', 3, 'LEGACY_FAILURE', 1, 6)
                    """.formatted(userId));
        }

        Flyway latest = flyway(null);
        latest.migrate();
        latest.validate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            assertTenant(statement, "async_task", "id", 2700, tenantId);
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM async_task WHERE id = 2700 "
                            + "AND task_id = 'task-c13b-owner-only' AND document_id IS NULL "
                            + "AND owner_id = " + userId + " AND status = 'FAILED' AND progress = 73 "
                            + "AND execution_phase = 'TERMINAL' AND attempt_count = 3 "
                            + "AND failure_code = 'LEGACY_FAILURE' AND deleted = 1 AND version = 6"));
        }
    }

    @Test
    void freshInstallMigratesV1ThroughV13AndCreatesExactTenantIndexes() throws Exception {
        Flyway flyway = flyway(null);
        flyway.clean();
        flyway.migrate();
        flyway.validate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            assertEquals("13", currentMigrationVersion(statement));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = 'knowledge_base' "
                            + "AND column_name = 'vector_readiness' AND is_nullable = 'NO'"));
            for (String table : new String[]{
                    "document", "document_chunk", "kb_permission",
                    "qa_history", "qa_feedback", "async_task"}) {
                assertEquals(1, scalar(statement,
                        "SELECT COUNT(*) FROM information_schema.columns "
                                + "WHERE table_schema = DATABASE() AND table_name = '" + table + "' "
                                + "AND column_name = 'tenant_id' AND is_nullable = 'NO'"));
            }

            assertIndex(statement, "document", "uk_kb_content_hash", true,
                    "tenant_id,kb_id,content_hash");
            assertIndex(statement, "document", "idx_document_tenant_id", false,
                    "tenant_id,id");
            assertIndex(statement, "document", "idx_document_tenant_kb", false,
                    "tenant_id,kb_id");
            assertIndex(statement, "document_chunk", "uk_document_chunk_position", true,
                    "tenant_id,document_id,chunk_index");
            assertIndex(statement, "document_chunk", "idx_chunk_tenant_document", false,
                    "tenant_id,document_id");
            assertIndex(statement, "document_chunk", "idx_chunk_tenant_vector", false,
                    "tenant_id,vector_id");
            assertIndex(statement, "kb_permission", "uk_kb_user", true,
                    "tenant_id,kb_id,user_id");
            assertIndex(statement, "kb_permission", "idx_kb_permission_tenant_user", false,
                    "tenant_id,user_id");
            assertIndex(statement, "qa_history", "idx_history_tenant_id", false,
                    "tenant_id,id");
            assertIndex(statement, "qa_history", "idx_history_tenant_user_created", false,
                    "tenant_id,user_id,created_at");
            assertIndex(statement, "qa_history", "idx_history_tenant_kb", false,
                    "tenant_id,kb_id");
            assertIndex(statement, "qa_feedback", "uk_qa_feedback_qa_user", true,
                    "tenant_id,qa_id,user_id");
            assertIndex(statement, "qa_feedback", "idx_feedback_tenant_qa", false,
                    "tenant_id,qa_id");
            assertIndex(statement, "async_task", "uk_task_id", true,
                    "tenant_id,task_id");
            assertIndex(statement, "async_task", "idx_async_task_tenant_document", false,
                    "tenant_id,document_id");
            assertIndex(statement, "async_task", "idx_async_task_tenant_lease", false,
                    "tenant_id,lease_until");
            assertIndex(statement, "async_task", "idx_async_task_recovery", false,
                    "task_type,status,execution_phase,next_attempt_at");
            assertIndex(statement, "async_task", "idx_async_task_tenant_recovery", false,
                    "tenant_id,task_type,status,execution_phase,next_attempt_at");
        }

        assertEquals(0, flyway.migrate().migrationsExecuted);
        flyway.validate();
    }

    @Test
    void v10UpgradeFailsClosedWhenPermissionUserBelongsToAnotherTenant() throws Exception {
        Flyway v10 = flyway(MigrationVersion.fromVersion("10"));
        v10.clean();
        v10.migrate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            long tenantA = scalar(statement, "SELECT id FROM tenant WHERE code = 'legacy-default'");
            long userA = scalar(statement, "SELECT id FROM `user` WHERE username = 'admin'");
            statement.executeUpdate("""
                    INSERT INTO tenant (id, code, name, enabled)
                    VALUES (2310, 'tenant-b', 'Tenant B', 1)
                    """);
            statement.executeUpdate("""
                    INSERT INTO `user` (id, tenant_id, username, password_hash, enabled)
                    VALUES (2311, 2310, 'tenant-b-user', 'test-only', 1)
                    """);
            statement.executeUpdate("""
                    INSERT INTO knowledge_base
                        (id, tenant_id, name, owner_id, vector_collection, document_count, is_public)
                    VALUES (2312, %d, 'tenant-a-kb', %d, 'kb_c13b_2312', 0, 0)
                    """.formatted(tenantA, userA));
            statement.executeUpdate("""
                    INSERT INTO kb_permission (id, kb_id, user_id, permission_type)
                    VALUES (2313, 2312, 2311, 'READ')
                    """);
        }

        assertMigrationFailsBeforePermanentColumns("chk_c13b_permission_parent");
    }

    @Test
    void v10UpgradeFailsBeforePermanentDdlForOwnerlessTask() throws Exception {
        Flyway v10 = flyway(MigrationVersion.fromVersion("10"));
        v10.clean();
        v10.migrate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO async_task (task_id, task_type, status, progress)
                    VALUES ('task-c13b-ownerless', 'LEGACY_GENERIC', 'PENDING', 0)
                    """);
        }

        assertMigrationFailsBeforePermanentColumns("chk_c13b_task_parent");
    }

    @Test
    void v10UpgradeFailsBeforePermanentDdlForCrossTenantDocumentUploader() throws Exception {
        Flyway v10 = flyway(MigrationVersion.fromVersion("10"));
        v10.clean();
        v10.migrate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            long tenantA = scalar(statement, "SELECT id FROM tenant WHERE code = 'legacy-default'");
            long userA = scalar(statement, "SELECT id FROM `user` WHERE username = 'admin'");
            statement.executeUpdate("""
                    INSERT INTO tenant (id, code, name, enabled)
                    VALUES (2410, 'tenant-document-b', 'Tenant Document B', 1)
                    """);
            statement.executeUpdate("""
                    INSERT INTO `user` (id, tenant_id, username, password_hash, enabled)
                    VALUES (2411, 2410, 'tenant-document-b-user', 'test-only', 1)
                    """);
            statement.executeUpdate("""
                    INSERT INTO knowledge_base
                        (id, tenant_id, name, owner_id, vector_collection, document_count)
                    VALUES (2412, %d, 'tenant-a-document-kb', %d, 'kb_c13b_2412', 1)
                    """.formatted(tenantA, userA));
            statement.executeUpdate("""
                    INSERT INTO document
                        (id, kb_id, uploader_id, title, file_type, status)
                    VALUES (2413, 2412, 2411, 'cross-tenant-uploader', 'md', 'PENDING')
                    """);
        }

        assertMigrationFailsBeforePermanentColumns("chk_c13b_document_parent");
    }

    @Test
    void v10UpgradeFailsBeforePermanentDdlForMissingRootTenant() throws Exception {
        Flyway v10 = flyway(MigrationVersion.fromVersion("10"));
        v10.clean();
        v10.migrate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO `user` (id, tenant_id, username, password_hash, enabled)
                    VALUES (2510, 999999, 'orphan-tenant-user', 'test-only', 1)
                    """);
        }

        assertMigrationFailsBeforePermanentColumns("chk_c13b_root_tenant");
    }

    private static void assertMigrationFailsBeforePermanentColumns(String errorCategory) throws Exception {
        FlywayException failure = assertThrows(FlywayException.class, () -> flyway(null).migrate());
        assertTrue(allMessages(failure).contains(errorCategory),
                () -> "Expected migration error category " + errorCategory + " but got: " + allMessages(failure));

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            assertEquals(0, scalar(statement,
                    "SELECT COUNT(*) FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = 'document' "
                            + "AND column_name = 'tenant_id'"));
        }
    }

    private static String allMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getMessage() != null) {
                messages.append(current.getMessage()).append('\n');
            }
        }
        return messages.toString();
    }

    private static void assertTenant(Statement statement,
            String table,
            String idColumn,
            long id,
            long expectedTenantId) throws Exception {
        assertEquals(expectedTenantId, scalar(statement,
                "SELECT tenant_id FROM " + table + " WHERE " + idColumn + " = " + id));
    }

    private static void assertIndex(Statement statement,
            String table,
            String index,
            boolean unique,
            String expectedColumns) throws Exception {
        try (ResultSet result = statement.executeQuery(
                "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS index_columns, "
                        + "MIN(non_unique) AS non_unique "
                        + "FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = '" + table + "' "
                        + "AND index_name = '" + index + "' GROUP BY index_name")) {
            assertTrue(result.next(), () -> "Missing index " + table + "." + index);
            assertEquals(expectedColumns, result.getString("index_columns"));
            assertEquals(unique ? 0 : 1, result.getInt("non_unique"));
        }
    }

    private static String currentMigrationVersion(Statement statement) throws Exception {
        try (ResultSet result = statement.executeQuery(
                "SELECT version FROM flyway_schema_history WHERE success = 1 "
                        + "ORDER BY installed_rank DESC LIMIT 1")) {
            assertTrue(result.next());
            return result.getString(1);
        }
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
