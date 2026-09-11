package com.enterprise.rag.core.rag;

import com.enterprise.rag.common.util.RedisUtil;
import com.enterprise.rag.core.rag.generator.AnswerGenerator;
import com.enterprise.rag.core.rag.model.*;
import com.enterprise.rag.core.rag.query.*;
import com.enterprise.rag.core.rag.service.RAGServiceImpl;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Test-scope audit: actual ordinary QA control flow, no application context or external clients. */
public final class C18BudgetAudit {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> CANARY = Set.of("fact-001", "definition-001", "reasoning-001",
            "multi-hop-001", "no-answer-001");
    private static final QueryEngineImpl VARIANTS = new QueryEngineImpl(null, null, null, null, null, null);
    private static final TenantVectorScope SCOPE = new TenantVectorScope(1, 1, "synthetic_audit");

    public record Sample(String id, int initialVariants, List<Integer> askVariantsPerPass,
                         int askRetrievalPasses, int queryEmbeddingUpperBound) {}
    public record Audit(String schemaVersion, String evidenceKind, Map<String, String> sourceSha256,
                        List<Sample> samples, Map<String, Integer> canary, Map<String, Integer> full,
                        int providerCalls, boolean businessDataOutbound) {}

    public static Path repoRoot() {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("openspec/project.md"))) root = root.getParent();
        if (root == null) throw new IllegalStateException("C18_REPO_NOT_FOUND");
        return root;
    }

    public static Audit auditRelease() throws Exception {
        Path root = repoRoot();
        List<Sample> samples = new ArrayList<>();
        for (String line : Files.readAllLines(root.resolve("docs/eval/releases/rag-eval-dev-v2.jsonl"))) {
            if (line.isBlank()) continue;
            var node = JSON.readTree(line);
            String question = node.get("question").asText();
            int initial = VARIANTS.explainQueryVariants(question).size();
            // All-empty branch visits every reachable explanatory fallback, not just the first hit.
            List<Integer> passes = exerciseAsk(question, Integer.MAX_VALUE);
            assertEquals(initial, passes.get(0));
            // Every early-exit position must be bounded by the all-empty path and generate exactly once.
            for (int hit = 1; hit <= passes.size(); hit++) {
                assertEquals(passes.subList(0, hit), exerciseAsk(question, hit));
            }
            samples.add(new Sample(node.get("id").asText(), initial, List.copyOf(passes), passes.size(),
                    initial + passes.stream().mapToInt(Integer::intValue).sum()));
        }
        Map<String, String> hashes = new TreeMap<>();
        for (String path : List.of(
                "docs/eval/dataset-manifest.json", "docs/eval/releases/rag-eval-dev-v2.jsonl",
                "rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java",
                "rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java",
                "rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java",
                "rag-core/src/main/java/com/enterprise/rag/core/rag/generator/LLMProperties.java",
                "rag-core/src/main/java/com/enterprise/rag/core/rag/prompt/PromptBuilder.java",
                "rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java",
                "rag-common/src/main/java/com/enterprise/rag/common/ratelimit/RateLimitInterceptor.java",
                "rag-common/src/main/java/com/enterprise/rag/common/ratelimit/SlidingWindowRateLimiter.java",
                "rag-admin/src/main/resources/application.yml",
                "rag-core/src/test/java/com/enterprise/rag/core/rag/C18BudgetAudit.java",
                "scripts/run_rag_eval.py", "scripts/run_reproducible_rag_eval.py")) {
            // Git-normalized text identity, independent of Windows checkout EOL policy.
            byte[] bytes = Files.readString(root.resolve(path)).replace("\r\n", "\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            hashes.put(path, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        }
        return new Audit("c18-query-budget-audit-v1", "OFFLINE_CALL_GRAPH_NOT_LIVE_QUALITY", hashes,
                List.copyOf(samples), budget(samples.stream().filter(s -> CANARY.contains(s.id())).toList()),
                budget(samples), 0, false);
    }

    private static List<Integer> exerciseAsk(String question, int hitAt) {
        QueryEngine query = mock(QueryEngine.class);
        AnswerGenerator generator = mock(AnswerGenerator.class);
        RedisUtil redis = mock(RedisUtil.class);
        List<Integer> passes = new ArrayList<>();
        AtomicInteger generation = new AtomicInteger();
        when(generator.getModelName()).thenReturn("synthetic");
        when(generator.generate(anyString(), anyList())).thenAnswer(call -> {
            generation.incrementAndGet();
            return GeneratedAnswer.of("Synthetic audit response.", List.of());
        });
        when(query.retrieveWithDiagnostics(anyString(), any(RetrieveOptions.class))).thenAnswer(call -> {
            RetrieveOptions options = call.getArgument(1);
            assertTrue(options.enableRerank());
            passes.add(VARIANTS.explainQueryVariants(call.getArgument(0)).size());
            return RetrievalResult.complete(passes.size() == hitAt
                    ? List.of(new RetrievedContext("Synthetic context.", "synthetic", 1f)) : List.of());
        });
        QAResponse response = new RAGServiceImpl(query, generator, redis, JSON)
                .ask(new QARequest(question, SCOPE, 5, 0.3f, Map.of(), false, false));
        assertNotEquals("error", response.metadata().get("status"));
        assertEquals(hitAt == Integer.MAX_VALUE ? 0 : 1, generation.get());
        if (hitAt == Integer.MAX_VALUE) assertEquals("no_result", response.metadata().get("status"));
        verifyNoInteractions(redis);
        return passes;
    }

    private static Map<String, Integer> budget(List<Sample> samples) {
        int initial = samples.stream().mapToInt(Sample::initialVariants).sum();
        int query = samples.stream().mapToInt(Sample::queryEmbeddingUpperBound).sum();
        return Map.of("debugRetrieval", samples.size(), "ask", samples.size(),
                "generationUpperBound", samples.size(), "initialVariants", initial,
                "explanatoryFallbackVariants", query - 2 * initial,
                "queryEmbeddingUpperBound", query, "judge", 0, "modelRerank", 0);
    }

    public static void writeIfRequested(Audit audit) throws Exception {
        String output = System.getProperty("c18.auditOutput");
        if (output == null) return;
        Path path = Path.of(output).toAbsolutePath().normalize();
        if (!path.startsWith(repoRoot().resolve("tmp/eval/c18")))
            throw new IllegalArgumentException("C18_AUDIT_OUTPUT_MUST_BE_LOCAL_TMP");
        Files.createDirectories(path.getParent());
        Files.writeString(path, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(audit) + "\n",
                StandardOpenOption.CREATE_NEW);
    }
}
