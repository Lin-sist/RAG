package com.enterprise.rag.core.rag.keyword;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryBm25KeywordIndexTest {

    @Test
    void shouldRetrieveChineseKeywordMatchesWithBm25Score() {
        InMemoryBm25KeywordIndex index = new InMemoryBm25KeywordIndex();
        index.upsert("kb_test", List.of(
                new KeywordDocument("a", "Spring IOC 和依赖注入基础", Map.of("kbId", 1)),
                new KeywordDocument("b", "缓存穿透 是指查询不存在的数据导致请求打到数据库", Map.of("kbId", 1))));

        List<RetrievedContext> results = index.search("kb_test", "什么是缓存穿透", 5, Map.of("kbId", 1));

        assertEquals(1, results.size());
        assertEquals("b", results.get(0).source());
        assertTrue(results.get(0).relevanceScore() > 0f);
    }

    @Test
    void shouldDeleteKeywordDocumentsByVectorId() {
        InMemoryBm25KeywordIndex index = new InMemoryBm25KeywordIndex();
        index.upsert("kb_test", List.of(
                new KeywordDocument("a", "缓存穿透 说明", Map.of()),
                new KeywordDocument("b", "分布式锁 说明", Map.of())));

        index.delete("kb_test", List.of("a"));

        List<RetrievedContext> results = index.search("kb_test", "缓存穿透", 5, Map.of());
        assertTrue(results.isEmpty());
    }
}
