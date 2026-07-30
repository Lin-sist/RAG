package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.util.RedisUtil;
import com.enterprise.rag.core.rag.generator.AnswerGenerator;
import com.enterprise.rag.core.rag.generator.GenerationBudget;
import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.QAResponse;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.query.RetrievalResult;
import com.enterprise.rag.core.rag.router.BoundedQueryRouter;
import com.enterprise.rag.core.rag.router.DeterministicFactIntentClassifier;
import com.enterprise.rag.core.rag.router.RouterProperties;
import com.enterprise.rag.core.rag.service.RAGServiceImpl;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class C16McpRouterIntegrationTest {

    @Test
    void mcpAskUsesTheSameBoundedFactRouteWithoutExposingClientSelectors() {
        TenantVectorScope scope = new TenantVectorScope(7L, 11L, "kb_router_fixture");
        RetrievedContext context = new RetrievedContext(
                "JWT 是一种紧凑的声明传输格式。",
                "chunk-1",
                0.91f,
                Map.of("documentId", 3L, "chunkId", "chunk-1"));
        Citation citation = Citation.grounded(
                "chunk-1", 3L, "chunk-1", 0.91d, context.content(), -1, -1);
        QueryEngine queryEngine = mock(QueryEngine.class);
        when(queryEngine.retrieveWithDiagnostics(eq("什么是 JWT？"), any())).thenReturn(
                new RetrievalResult(
                        List.of(context),
                        Map.of(
                                "queryVariantCount", 1,
                                "rerankModelCallCount", 0,
                                "rerankCandidateCount", 1)));
        AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
        when(answerGenerator.getModelName()).thenReturn("deterministic-fixture");
        when(answerGenerator.generate(
                eq("什么是 JWT？"), eq(List.of(context)), any(GenerationBudget.class)))
                .thenReturn(GeneratedAnswer.of(
                        "JWT 是一种紧凑的声明传输格式。",
                        List.of(citation),
                        Map.of(
                                "validCitations", 1,
                                "estimatedContextTokens", 100,
                                "estimatedOutputTokens", 20,
                                "generationModelCallCount", 0)));
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        RAGServiceImpl ragService = new RAGServiceImpl(
                queryEngine,
                answerGenerator,
                mock(RedisUtil.class),
                new ObjectMapper(),
                new BoundedQueryRouter(properties, new DeterministicFactIntentClassifier()));

        AuthorizationService authorization = mock(AuthorizationService.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        when(knowledgeBaseService.requireReadyVectorScope(7L, 11L)).thenReturn(scope);
        McpCitationReader citationReader = mock(McpCitationReader.class);
        when(citationReader.read(any(), eq(11L), eq(3L), eq("chunk-1"))).thenReturn(
                new McpCitationReader.CitationSource(
                        11L, 3L, "chunk-1", 0, "JWT 文档", -1, -1,
                        context.content(), false,
                        "rag://knowledge-bases/11/documents/3/chunks/0"));
        McpExternalReadService facade = new McpExternalReadService(
                authorization,
                knowledgeBaseService,
                queryEngine,
                ragService,
                citationReader,
                false);

        QAResponse sync = ragService.ask(new QARequest(
                "什么是 JWT？", scope, 5, QARequest.DEFAULT_MIN_SCORE, Map.of(), false, false));
        Map<String, Object> mcp = facade.ask(
                new RequestIdentity(5L, 7L), 11L, "什么是 JWT？", 5, QARequest.DEFAULT_MIN_SCORE);

        assertEquals("fact-intent-v1", sync.metadata().get("routeClassifierVersion"));
        assertEquals("fact-v1", sync.metadata().get("routeEffectiveStrategy"));
        assertEquals("evidence-no-answer-v1", sync.metadata().get("routePolicyVersion"));
        assertEquals("ANSWER", sync.metadata().get("routeFinalState"));
        assertEquals("NONE", sync.metadata().get("noAnswerReason"));
        assertEquals(1, sync.metadata().get("routeRetrievalPasses"));
        assertEquals(1, sync.metadata().get("routeGenerationCalls"));
        assertEquals(0, sync.metadata().get("generationModelCallCount"));

        assertEquals("ok", mcp.get("status"));
        assertEquals(sync.answer(), mcp.get("answer"));
        assertEquals(1, ((List<?>) mcp.get("citations")).size());
        Map<?, ?> diagnostics = (Map<?, ?>) mcp.get("diagnostics");
        assertEquals(0L, diagnostics.get("generationModelCallCount"));
        assertFalse(diagnostics.containsKey("routeEffectiveStrategy"));
        assertFalse(diagnostics.containsKey("budget"));
        assertFalse(diagnostics.containsKey("provider"));
        verify(queryEngine, times(2)).retrieveWithDiagnostics(eq("什么是 JWT？"), any());
    }
}
