package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.QAResponse;
import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.query.RetrievalResult;
import com.enterprise.rag.core.rag.service.RAGService;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Read-only search/ask facade that deliberately bypasses REST history/count side effects. */
final class McpExternalReadService {

    private static final int MAX_EXCERPT_BYTES = 8_192;
    private static final int MAX_ANSWER_BYTES = 32_768;
    private static final int MAX_DISPLAY_CHARS = 512;
    private static final int MAX_DIAGNOSTIC_STRING_CHARS = 128;
    private static final Set<String> STRING_DIAGNOSTICS = Set.of(
            "retrievalMode",
            "degradedDependency",
            "rerankRequestedProvider",
            "rerankEffectiveProvider",
            "rerankFallbackReason",
            "rerankModel",
            "rerankProtocol",
            "generationRequestedProvider",
            "generationEffectiveProvider",
            "generationFallbackReason",
            "generationModel");
    private static final Set<String> INTEGER_DIAGNOSTICS = Set.of(
            "rerankFallbackCount",
            "rerankModelCallCount",
            "rerankCandidateCount",
            "rerankScoredCount",
            "rerankLatencyMillis",
            "generationFallbackCount",
            "generationModelCallCount",
            "generationRetryCount");
    private static final Set<String> BOOLEAN_DIAGNOSTICS = Set.of(
            "retrievalDegraded", "generationTimedOut", "cacheHit");
    private static final Set<String> RATIO_DIAGNOSTICS = Set.of(
            "rerankCoverage", "citationCoverage");

    private final AuthorizationService authorizationService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final QueryEngine queryEngine;
    private final RAGService ragService;
    private final McpCitationReader citationReader;
    private final boolean cacheEnabled;

    McpExternalReadService(
            AuthorizationService authorizationService,
            KnowledgeBaseService knowledgeBaseService,
            QueryEngine queryEngine,
            RAGService ragService,
            McpCitationReader citationReader,
            boolean cacheEnabled) {
        this.authorizationService = authorizationService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.queryEngine = queryEngine;
        this.ragService = ragService;
        this.citationReader = citationReader;
        this.cacheEnabled = cacheEnabled;
    }

    Map<String, Object> search(
            RequestIdentity identity,
            long kbId,
            String query,
            int topK,
            float minScore) {
        TenantVectorScope scope = requireReadyScope(identity, kbId);
        RetrievalResult retrieval;
        try {
            retrieval = queryEngine.retrieveWithDiagnostics(
                    query,
                    new RetrieveOptions(scope, topK, minScore, Map.of(), true));
        } catch (VectorDependencyException exception) {
            throw new McpToolExecutionException("MCP_DEPENDENCY_UNAVAILABLE");
        } catch (RuntimeException exception) {
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        }
        if (retrieval == null) {
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        }

        List<Map<String, Object>> items = new ArrayList<>();
        List<RetrievedContext> contexts = retrieval.contexts();
        for (int index = 0; index < contexts.size() && items.size() < topK; index++) {
            RetrievedContext context = contexts.get(index);
            if (context == null) {
                continue;
            }
            SourceIdentity sourceIdentity = sourceIdentity(context.metadata());
            if (sourceIdentity == null) {
                continue;
            }
            McpCitationReader.CitationSource source;
            try {
                source = citationReader.read(
                        identity, kbId, sourceIdentity.documentId(), sourceIdentity.chunkId());
            } catch (McpToolExecutionException exception) {
                throw new McpToolExecutionException("MCP_DEPENDENCY_UNAVAILABLE");
            }
            McpUtf8Bounder.BoundedText excerpt;
            try {
                excerpt = McpUtf8Bounder.bound(
                        context.content() == null ? "" : context.content(),
                        MAX_EXCERPT_BYTES);
            } catch (IllegalArgumentException exception) {
                throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", index + 1);
            item.put("score", safeScore(context.relevanceScore()));
            item.put("documentId", source.documentId());
            item.put("chunkId", source.chunkId());
            item.put("documentTitle", source.title());
            item.put("sourceFileName", sourceFileName(context.metadata(), source.title()));
            item.put("boundedExcerpt", excerpt.text());
            item.put("resourceUri", source.resourceUri());
            items.add(Map.copyOf(item));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", items.isEmpty() ? "no_result" : "ok");
        payload.put("resultCount", items.size());
        payload.put("items", List.copyOf(items));
        payload.put("diagnostics", sanitizeDiagnostics(retrieval.diagnostics()));
        return Map.copyOf(payload);
    }

    Map<String, Object> ask(
            RequestIdentity identity,
            long kbId,
            String question,
            int topK,
            float minScore) {
        TenantVectorScope scope = requireReadyScope(identity, kbId);
        QAResponse response;
        try {
            response = ragService.ask(new QARequest(
                    question,
                    scope,
                    topK,
                    minScore,
                    Map.of(),
                    cacheEnabled,
                    false));
        } catch (RuntimeException exception) {
            throw new McpToolExecutionException("MCP_DEPENDENCY_UNAVAILABLE");
        }
        if (response == null) {
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        }

        String upstreamStatus = response.metadata() == null
                ? null
                : String.valueOf(response.metadata().get("status"));
        if ("error".equals(upstreamStatus) || !response.isSuccess()) {
            throw new McpToolExecutionException("MCP_DEPENDENCY_UNAVAILABLE");
        }
        String status = "no_result".equals(upstreamStatus) ? "no_result" : "ok";
        McpUtf8Bounder.BoundedText answer;
        try {
            answer = McpUtf8Bounder.bound(
                    response.answer() == null ? "" : response.answer(),
                    MAX_ANSWER_BYTES);
        } catch (IllegalArgumentException exception) {
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        }

        List<Map<String, Object>> citations = new ArrayList<>();
        List<Citation> upstreamCitations = response.citations() == null
                ? List.of()
                : response.citations();
        for (Citation citation : upstreamCitations) {
            if (citation == null || citation.documentId() == null
                    || citation.chunkId() == null || citation.chunkId().isBlank()) {
                continue;
            }
            McpCitationReader.CitationSource source;
            try {
                source = citationReader.read(
                        identity, kbId, citation.documentId(), citation.chunkId());
            } catch (McpToolExecutionException exception) {
                throw new McpToolExecutionException("MCP_DEPENDENCY_UNAVAILABLE");
            }
            String snippet = citation.snippet() == null
                    ? source.boundedContent()
                    : citation.snippet();
            McpUtf8Bounder.BoundedText boundedSnippet = McpUtf8Bounder.bound(
                    snippet, MAX_EXCERPT_BYTES);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("documentId", source.documentId());
            item.put("chunkId", source.chunkId());
            item.put("title", source.title());
            item.put("boundedSnippet", boundedSnippet.text());
            item.put("score", safeScore(citation.score()));
            item.put("resourceUri", source.resourceUri());
            citations.add(Map.copyOf(item));
            if (citations.size() >= 20) {
                break;
            }
        }

        Map<String, Object> diagnostics = new LinkedHashMap<>(
                sanitizeDiagnostics(response.metadata()));
        diagnostics.put("cacheHit", cacheEnabled
                && Boolean.TRUE.equals(response.metadata() == null
                        ? null
                        : response.metadata().get("cacheHit")));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("answer", answer.text());
        payload.put("citations", List.copyOf(citations));
        if ("no_result".equals(status)) {
            payload.put("errorCategory", "NO_RESULT");
        }
        payload.put("diagnostics", Map.copyOf(diagnostics));
        return Map.copyOf(payload);
    }

    private TenantVectorScope requireReadyScope(RequestIdentity identity, long kbId) {
        try {
            authorizationService.requireKnowledgeBaseReadAccess(kbId, identity);
            return knowledgeBaseService.requireReadyVectorScope(identity.tenantId(), kbId);
        } catch (BusinessException exception) {
            if (exception.getHttpStatus() == HttpStatus.NOT_FOUND) {
                throw new McpToolExecutionException("MCP_RESOURCE_NOT_FOUND");
            }
            if (exception.getHttpStatus() == HttpStatus.FORBIDDEN) {
                throw new McpToolExecutionException("MCP_FORBIDDEN");
            }
            if (exception.getHttpStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
                throw new McpToolExecutionException("MCP_DEPENDENCY_UNAVAILABLE");
            }
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        } catch (RuntimeException exception) {
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        }
    }

    private SourceIdentity sourceIdentity(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object documentId = metadata.get("documentId");
        Object chunkId = metadata.get("chunkId");
        if (!(documentId instanceof Number number)
                || !(chunkId instanceof String string)
                || string.isBlank()
                || string.length() > 100
                || !string.matches("[A-Za-z0-9:_-]+")) {
            return null;
        }
        return new SourceIdentity(number.longValue(), string);
    }

    private String sourceFileName(Map<String, Object> metadata, String fallback) {
        Object value = metadata == null ? null : metadata.get("sourceFileName");
        String name = value instanceof String string && !string.isBlank()
                ? string
                : fallback;
        name = name.replace('\\', '/');
        int pathSeparator = name.lastIndexOf('/');
        if (pathSeparator >= 0) {
            name = name.substring(pathSeparator + 1);
        }
        int driveOrSchemeSeparator = name.lastIndexOf(':');
        if (driveOrSchemeSeparator >= 0) {
            name = name.substring(driveOrSchemeSeparator + 1);
        }
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            name = "source";
        }
        return boundChars(name, MAX_DISPLAY_CHARS);
    }

    private Map<String, Object> sanitizeDiagnostics(Map<String, Object> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        diagnostics.forEach((key, value) -> {
            if (STRING_DIAGNOSTICS.contains(key) && value instanceof String string) {
                safe.put(key, boundChars(string, MAX_DIAGNOSTIC_STRING_CHARS));
            } else if (INTEGER_DIAGNOSTICS.contains(key)
                    && value instanceof Number number
                    && number.longValue() >= 0) {
                safe.put(key, number.longValue());
            } else if (BOOLEAN_DIAGNOSTICS.contains(key) && value instanceof Boolean) {
                safe.put(key, value);
            } else if (RATIO_DIAGNOSTICS.contains(key)
                    && value instanceof Number number
                    && Double.isFinite(number.doubleValue())
                    && number.doubleValue() >= 0.0d
                    && number.doubleValue() <= 1.0d) {
                safe.put(key, number.doubleValue());
            }
        });
        return Map.copyOf(safe);
    }

    private double safeScore(float score) {
        if (!Float.isFinite(score)) {
            return 0.0d;
        }
        return Math.max(0.0d, Math.min(1.0d, score));
    }

    private double safeScore(Double score) {
        if (score == null || !Double.isFinite(score)) {
            return 0.0d;
        }
        return Math.max(0.0d, Math.min(1.0d, score));
    }

    private String boundChars(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        int count = value.codePointCount(0, value.length());
        return count <= maxChars
                ? value
                : value.substring(0, value.offsetByCodePoints(0, maxChars));
    }

    private record SourceIdentity(long documentId, String chunkId) {
    }
}
