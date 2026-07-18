package com.enterprise.rag.admin.kb.task;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.enterprise.rag.admin.kb.mapper.IndexTaskMapper;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class C5RecoveryMySqlTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("rag_c5_recovery")
            .withUsername("rag_test")
            .withPassword("rag_test_password");

    @BeforeEach
    void migrateFreshDatabase() {
        Flyway flyway = flyway(null);
        flyway.clean();
        flyway.migrate();
        flyway.validate();
    }

    @Test
    void freshAndV1DatabasesMigrateToLatest() throws Exception {
        assertEquals("9", currentMigrationVersion());

        Flyway v1 = flyway(MigrationVersion.fromVersion("1"));
        v1.clean();
        v1.migrate();
        Flyway latest = flyway(null);
        latest.migrate();
        latest.validate();

        assertEquals("9", currentMigrationVersion());
    }

    @Test
    void v7LegacyLedgerAndDuplicateChunksRemainCompatible() throws Exception {
        Flyway v7 = flyway(MigrationVersion.fromVersion("7"));
        v7.clean();
        v7.migrate();
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO document
                        (id, kb_id, uploader_id, title, file_path, file_type, status,
                         input_size_bytes, input_sha256, input_state)
                    VALUES (700, 10, 20, 'legacy', 'objects/legacy', 'md', 'FAILED',
                            12, REPEAT('a', 64), 'AVAILABLE')
                    """);
            statement.executeUpdate("""
                    INSERT INTO document_chunk
                        (document_id, vector_id, content, chunk_index, deleted)
                    VALUES (700, 'old-a', 'a', 0, 0), (700, 'old-b', 'b', 0, 0)
                    """);
            statement.executeUpdate("""
                    INSERT INTO async_task (task_id, task_type, status, progress)
                    VALUES ('legacy-generic-task', 'LEGACY_GENERIC', 'PENDING', 0)
                    """);
        }

        Flyway latest = flyway(null);
        latest.migrate();
        latest.validate();

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            assertEquals(1, singleInt(statement,
                    "SELECT COUNT(*) FROM document_chunk WHERE document_id = 700 AND chunk_index = 0"));
            assertEquals(1, singleInt(statement,
                    "SELECT COUNT(*) FROM async_task WHERE task_id = 'legacy-generic-task' "
                            + "AND document_id IS NULL"));
            assertEquals(1, singleInt(statement,
                    "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics "
                            + "WHERE table_schema = DATABASE() AND table_name = 'document_chunk' "
                            + "AND index_name = 'uk_document_chunk_position' AND non_unique = 0"));
        }
        try (SqlSession session = sqlSessionFactory().openSession(true)) {
            DocumentMapper mapper = session.getMapper(DocumentMapper.class);
            assertTrue(mapper.findLegacyUnledgered(20).stream()
                    .anyMatch(document -> Long.valueOf(700L).equals(document.getId())));
            assertEquals(1, mapper.quarantineLegacyUnledgered(700L));
        }
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            assertEquals(1, singleInt(statement,
                    "SELECT COUNT(*) FROM document WHERE id = 700 "
                            + "AND status = 'RECONCILIATION_REQUIRED' AND input_state = 'AVAILABLE'"));
        }
    }

    @Test
    void twoClaimantsHeartbeatBackoffAndAttemptExhaustionUseMySqlFacts() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO async_task
                        (task_id, task_type, status, progress, execution_phase, attempt_count)
                    VALUES ('task-c5-real', 'DOCUMENT_INDEX', 'RUNNING', 30,
                            'SAFE_PRE_VECTOR', 0)
                    """);
        }

        SqlSessionFactory factory = sqlSessionFactory();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> workerA = pool.submit(() -> claim(factory, start, "worker-a"));
            Future<Boolean> workerB = pool.submit(() -> claim(factory, start, "worker-b"));
            start.countDown();
            assertEquals(1, List.of(workerA.get(), workerB.get()).stream().filter(Boolean::booleanValue).count());
        } finally {
            pool.shutdownNow();
        }

        String owner;
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT lease_owner FROM async_task WHERE task_id = 'task-c5-real'")) {
            assertTrue(result.next());
            owner = result.getString(1);
            assertNotNull(owner);
        }

        try (SqlSession session = factory.openSession(true)) {
            IndexTaskMapper mapper = session.getMapper(IndexTaskMapper.class);
            assertEquals(0, mapper.heartbeat("task-c5-real", "not-owner", 300));
            assertEquals(1, mapper.heartbeat("task-c5-real", owner, 300));
        }
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE async_task SET lease_until = DATE_SUB(NOW(6), INTERVAL 1 SECOND) "
                    + "WHERE task_id = 'task-c5-real'");
        }
        try (SqlSession session = factory.openSession(true)) {
            IndexTaskMapper mapper = session.getMapper(IndexTaskMapper.class);
            assertEquals(0, mapper.heartbeat("task-c5-real", owner, 300));
            assertEquals(1, mapper.claim("task-c5-real", "worker-retry", 300, 3));
            assertEquals(1, mapper.scheduleRetry(
                    "task-c5-real", "worker-retry", "INDEX_TASK_RECOVERY_FAILED", 30));
            assertEquals(0, mapper.claim("task-c5-real", "worker-too-early", 300, 3));
        }
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE async_task SET next_attempt_at = DATE_SUB(NOW(6), INTERVAL 1 SECOND) "
                    + "WHERE task_id = 'task-c5-real'");
        }
        try (SqlSession session = factory.openSession(true)) {
            IndexTaskMapper mapper = session.getMapper(IndexTaskMapper.class);
            assertEquals(1, mapper.claim("task-c5-real", "worker-final", 300, 3));
            assertEquals(1, mapper.markAttemptsExhausted(
                    "task-c5-real", "worker-final", "INDEX_TASK_RECOVERY_FAILED"));
            assertFalse(mapper.scanClaimable(20, 3).stream()
                    .anyMatch(task -> "task-c5-real".equals(task.getTaskId())));
        }
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT status, execution_phase, lease_owner, next_attempt_at "
                                + "FROM async_task WHERE task_id = 'task-c5-real'")) {
            assertTrue(result.next());
            assertEquals("FAILED", result.getString("status"));
            assertEquals("TERMINAL", result.getString("execution_phase"));
            assertEquals(null, result.getString("lease_owner"));
            assertEquals(null, result.getTimestamp("next_attempt_at"));
        }
    }

    @Test
    void sqlFinalizerIsIdempotentAndAllFactsCanRollbackTogether() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO knowledge_base
                        (id, name, owner_id, vector_collection, document_count)
                    VALUES (800, 'c5-finalize', 20, 'kb_c5_finalize', 0)
                    """);
            statement.executeUpdate("""
                    INSERT INTO document
                        (id, kb_id, uploader_id, title, file_path, file_type, status,
                         input_size_bytes, input_sha256, input_state)
                    VALUES (800, 800, 20, 'finalize', 'objects/finalize', 'md', 'PROCESSING',
                            12, REPEAT('b', 64), 'AVAILABLE')
                    """);
            statement.executeUpdate("""
                    INSERT INTO async_task
                        (task_id, task_type, status, progress, document_id,
                         execution_phase, attempt_count)
                    VALUES ('task-finalize-real', 'DOCUMENT_INDEX', 'RUNNING', 85, 800,
                            'FINALIZING', 1)
                    """);
        }
        DocumentChunk chunk = chunk(800L, "vector-800-0");
        SqlSessionFactory factory = sqlSessionFactory();
        finalizeAndCommit(factory, "task-finalize-real", 800L, 800L, "hash-800", chunk);
        finalizeAndCommit(factory, "task-finalize-real", 800L, 800L, "hash-800", chunk);

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            assertEquals(1, singleInt(statement,
                    "SELECT COUNT(*) FROM document_chunk WHERE document_id = 800"));
            assertEquals(1, singleInt(statement,
                    "SELECT document_count FROM knowledge_base WHERE id = 800"));
            assertEquals(1, singleInt(statement,
                    "SELECT COUNT(*) FROM async_task WHERE task_id = 'task-finalize-real' "
                            + "AND status = 'COMPLETED' AND execution_phase = 'TERMINAL'"));

            statement.executeUpdate("""
                    INSERT INTO document
                        (id, kb_id, uploader_id, title, file_path, file_type, status,
                         input_size_bytes, input_sha256, input_state)
                    VALUES (801, 800, 20, 'rollback', 'objects/rollback', 'md', 'PROCESSING',
                            12, REPEAT('c', 64), 'AVAILABLE')
                    """);
            statement.executeUpdate("""
                    INSERT INTO async_task
                        (task_id, task_type, status, progress, document_id,
                         execution_phase, attempt_count)
                    VALUES ('task-finalize-rollback', 'DOCUMENT_INDEX', 'RUNNING', 80, 801,
                            'SAFE_PRE_VECTOR', 1)
                    """);
        }

        try (SqlSession session = factory.openSession(false)) {
            IndexTaskSqlFinalizer finalizer = finalizer(session);
            assertThrows(IllegalStateException.class,
                    () -> finalizer.finalizeSql(
                            "task-finalize-rollback", 800L, 801L, "hash-801",
                            List.of(chunk(801L, "vector-801-0"))));
            session.rollback();
        }

        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement()) {
            assertEquals(0, singleInt(statement,
                    "SELECT COUNT(*) FROM document_chunk WHERE document_id = 801"));
            assertEquals(1, singleInt(statement,
                    "SELECT document_count FROM knowledge_base WHERE id = 800"));
            assertEquals(1, singleInt(statement,
                    "SELECT COUNT(*) FROM document WHERE id = 801 AND status = 'PROCESSING'"));
        }
        assertTrue(IndexTaskSqlFinalizer.class
                .getMethod("finalizeSql", String.class, long.class, long.class, String.class, List.class)
                .isAnnotationPresent(Transactional.class));
    }

    private static boolean claim(SqlSessionFactory factory, CountDownLatch start, String worker) throws Exception {
        start.await();
        try (SqlSession session = factory.openSession(true)) {
            return session.getMapper(IndexTaskMapper.class)
                    .claim("task-c5-real", worker, 300, 3) == 1;
        }
    }

    private static SqlSessionFactory sqlSessionFactory() {
        UnpooledDataSource dataSource = new UnpooledDataSource(
                MYSQL.getDriverClassName(), MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        Environment environment = new Environment(
                "c5-real-mysql", new JdbcTransactionFactory(), dataSource);
        MybatisConfiguration configuration = new MybatisConfiguration(environment);
        configuration.addMapper(IndexTaskMapper.class);
        configuration.addMapper(DocumentMapper.class);
        configuration.addMapper(DocumentChunkMapper.class);
        configuration.addMapper(KnowledgeBaseMapper.class);
        return new MybatisSqlSessionFactoryBuilder().build(configuration);
    }

    private static void finalizeAndCommit(SqlSessionFactory factory,
            String taskId,
            long kbId,
            long documentId,
            String contentHash,
            DocumentChunk chunk) {
        try (SqlSession session = factory.openSession(false)) {
            finalizer(session).finalizeSql(
                    taskId, kbId, documentId, contentHash, List.of(chunk));
            session.commit();
        }
    }

    private static IndexTaskSqlFinalizer finalizer(SqlSession session) {
        return new IndexTaskSqlFinalizer(
                session.getMapper(DocumentMapper.class),
                session.getMapper(DocumentChunkMapper.class),
                session.getMapper(KnowledgeBaseMapper.class),
                session.getMapper(IndexTaskMapper.class));
    }

    private static DocumentChunk chunk(long documentId, String vectorId) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(documentId);
        chunk.setVectorId(vectorId);
        chunk.setContent("content");
        chunk.setChunkIndex(0);
        chunk.setStartPos(0);
        chunk.setEndPos(7);
        return chunk;
    }

    private static int singleInt(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static String currentMigrationVersion() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT version FROM flyway_schema_history WHERE success = 1 "
                                + "ORDER BY installed_rank DESC LIMIT 1")) {
            assertTrue(result.next());
            return result.getString(1);
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
