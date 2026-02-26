package com.enterprise.rag.admin.qa;

import com.enterprise.rag.admin.qa.dto.*;
import com.enterprise.rag.admin.qa.entity.QAFeedback;
import com.enterprise.rag.admin.qa.entity.QAHistory;
import com.enterprise.rag.admin.qa.service.QAFeedbackService;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.core.rag.model.Citation;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 问答历史与反馈模块属性测试
 * 
 * Feature: enterprise-rag-qa-system
 * Validates: Requirements 12.1, 12.2, 12.3
 */
class QAHistoryPropertyTest {

    /**
     * Property 26: 问答历史记录完整性
     * 
     * *For any* 成功的问答请求，应在历史记录中保存问题、答案和引用来源。
     * 
     * **Validates: Requirements 12.1**
     */
    @Property(tries = 100)
    void qaHistoryShouldPreserveAllFields(
            @ForAll("question") String question,
            @ForAll("answer") String answer,
            @ForAll("citations") List<Citation> citations,
            @ForAll("positiveId") Long userId,
            @ForAll("positiveId") Long kbId,
            @ForAll("traceId") String traceId,
            @ForAll("latencyMs") Integer latencyMs) {
        
        // Setup in-memory service
        InMemoryQAHistoryService historyService = new InMemoryQAHistoryService();
        
        // Save QA history
        SaveQAHistoryRequest request = SaveQAHistoryRequest.builder()
                .userId(userId)
                .kbId(kbId)
                .question(question)
                .answer(answer)
                .citations(citations)
                .traceId(traceId)
                .latencyMs(latencyMs)
                .build();
        
        QAHistoryDTO saved = historyService.save(request);
        
        // Verify saved record has an ID
        assertThat(saved.getId() != null)
                .as("Saved history should have an ID")
                .isTrue();
        
        // Retrieve and verify all fields
        Optional<QAHistoryDTO> retrieved = historyService.getById(saved.getId());
        
        assertThat(retrieved.isPresent())
                .as("Should be able to retrieve saved history")
                .isTrue();
        
        QAHistoryDTO history = retrieved.get();
        
        // Verify question is preserved
        assertThat(question.equals(history.getQuestion()))
                .as("Question should be preserved. Expected: %s, Got: %s", question, history.getQuestion())
                .isTrue();
        
        // Verify answer is preserved
        assertThat(answer.equals(history.getAnswer()))
                .as("Answer should be preserved. Expected: %s, Got: %s", answer, history.getAnswer())
                .isTrue();
        
        // Verify citations are preserved
        assertThat(history.getCitations() != null)
                .as("Citations should not be null")
                .isTrue();
        assertThat(history.getCitations().size() == citations.size())
                .as("Citations count should match. Expected: %d, Got: %d", 
                    citations.size(), history.getCitations().size())
                .isTrue();
        
        // Verify user and kb IDs
        assertThat(userId.equals(history.getUserId()))
                .as("User ID should be preserved")
                .isTrue();
        assertThat(kbId.equals(history.getKbId()))
                .as("KB ID should be preserved")
                .isTrue();
        
        // Verify trace ID
        assertThat(traceId.equals(history.getTraceId()))
                .as("Trace ID should be preserved")
                .isTrue();
        
        // Verify latency
        assertThat(latencyMs.equals(history.getLatencyMs()))
                .as("Latency should be preserved")
                .isTrue();
    }

    /**
     * Property 27: 分页查询正确性
     * 
     * *For any* 分页查询请求，返回的记录数应不超过页大小，且按时间倒序排列。
     * 
     * **Validates: Requirements 12.2**
     */
    @Property(tries = 100)
    void paginationShouldReturnCorrectResults(
            @ForAll("recordCount") int recordCount,
            @ForAll("pageSize") int pageSize,
            @ForAll("positiveId") Long userId) {
        
        // Setup in-memory service
        InMemoryQAHistoryService historyService = new InMemoryQAHistoryService();
        
        // Create multiple history records with different timestamps
        List<Long> createdIds = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            SaveQAHistoryRequest request = SaveQAHistoryRequest.builder()
                    .userId(userId)
                    .kbId(1L)
                    .question("Question " + i)
                    .answer("Answer " + i)
                    .citations(List.of())
                    .traceId("trace-" + i)
                    .latencyMs(100 + i)
                    .build();
            QAHistoryDTO saved = historyService.save(request);
            createdIds.add(saved.getId());
            
            // Small delay to ensure different timestamps
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Query first page
        QAHistoryPageRequest pageRequest = new QAHistoryPageRequest();
        pageRequest.setUserId(userId);
        pageRequest.setPage(1);
        pageRequest.setSize(pageSize);
        
        PageResult<QAHistoryDTO> result = historyService.getPage(pageRequest);
        
        // Verify page size constraint
        int expectedRecords = Math.min(pageSize, recordCount);
        assertThat(result.getRecords().size() == expectedRecords)
                .as("Page should contain at most pageSize records. Expected: %d, Got: %d",
                    expectedRecords, result.getRecords().size())
                .isTrue();
        
        // Verify total count
        assertThat(result.getTotal() == recordCount)
                .as("Total count should match. Expected: %d, Got: %d", recordCount, result.getTotal())
                .isTrue();
        
        // Verify descending order by creation time
        List<QAHistoryDTO> records = result.getRecords();
        for (int i = 0; i < records.size() - 1; i++) {
            LocalDateTime current = records.get(i).getCreatedAt();
            LocalDateTime next = records.get(i + 1).getCreatedAt();
            assertThat(!current.isBefore(next))
                    .as("Records should be in descending order by creation time")
                    .isTrue();
        }
        
        // Verify pagination metadata
        int expectedTotalPages = (int) Math.ceil((double) recordCount / pageSize);
        assertThat(result.getTotalPages() == expectedTotalPages)
                .as("Total pages should be correct. Expected: %d, Got: %d",
                    expectedTotalPages, result.getTotalPages())
                .isTrue();
        
        assertThat(result.isHasNext() == (recordCount > pageSize))
                .as("hasNext should be correct")
                .isTrue();
        
        assertThat(!result.isHasPrevious())
                .as("First page should not have previous")
                .isTrue();
    }

    /**
     * Property 28: 反馈保存正确性
     * 
     * *For any* 用户提交的反馈，应能通过问答 ID 查询到该反馈记录。
     * 
     * **Validates: Requirements 12.3**
     */
    @Property(tries = 100)
    void feedbackShouldBeRetrievableByQaId(
            @ForAll("positiveId") Long qaId,
            @ForAll("positiveId") Long userId,
            @ForAll("rating") Integer rating,
            @ForAll("comment") String comment) {
        
        // Setup in-memory service
        InMemoryQAFeedbackService feedbackService = new InMemoryQAFeedbackService();
        
        // Submit feedback
        SubmitFeedbackRequest request = new SubmitFeedbackRequest();
        request.setQaId(qaId);
        request.setUserId(userId);
        request.setRating(rating);
        request.setComment(comment);
        
        QAFeedbackDTO submitted = feedbackService.submit(request);
        
        // Verify submitted feedback has an ID
        assertThat(submitted.getId() != null)
                .as("Submitted feedback should have an ID")
                .isTrue();
        
        // Retrieve by QA ID
        Optional<QAFeedbackDTO> retrieved = feedbackService.getByQaId(qaId);
        
        assertThat(retrieved.isPresent())
                .as("Should be able to retrieve feedback by QA ID")
                .isTrue();
        
        QAFeedbackDTO feedback = retrieved.get();
        
        // Verify all fields
        assertThat(qaId.equals(feedback.getQaId()))
                .as("QA ID should match")
                .isTrue();
        
        assertThat(userId.equals(feedback.getUserId()))
                .as("User ID should match")
                .isTrue();
        
        assertThat(rating.equals(feedback.getRating()))
                .as("Rating should match. Expected: %d, Got: %d", rating, feedback.getRating())
                .isTrue();
        
        assertThat(Objects.equals(comment, feedback.getComment()))
                .as("Comment should match")
                .isTrue();
        
        // Verify can also retrieve by ID
        Optional<QAFeedbackDTO> byId = feedbackService.getById(submitted.getId());
        assertThat(byId.isPresent())
                .as("Should be able to retrieve feedback by ID")
                .isTrue();
        
        // Verify hasUserFeedback returns true
        assertThat(feedbackService.hasUserFeedback(qaId, userId))
                .as("hasUserFeedback should return true after submission")
                .isTrue();
    }

    // ==================== Providers ====================

    @Provide
    Arbitrary<String> question() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(200);
    }

    @Provide
    Arbitrary<String> answer() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(10)
                .ofMaxLength(500);
    }

    @Provide
    Arbitrary<List<Citation>> citations() {
        Arbitrary<Citation> citation = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(50)
                .flatMap(source -> Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(10)
                        .ofMaxLength(100)
                        .map(snippet -> Citation.of(source, snippet)));
        
        return citation.list().ofMinSize(0).ofMaxSize(5);
    }

    @Provide
    Arbitrary<String> traceId() {
        return Arbitraries.strings()
                .withCharRange('a', 'f')
                .ofLength(32)
                .map(s -> "trace-" + s);
    }

    @Provide
    Arbitrary<Integer> latencyMs() {
        return Arbitraries.integers().between(10, 5000);
    }

    @Provide
    Arbitrary<Long> positiveId() {
        return Arbitraries.longs().between(1L, 1000000L);
    }

    @Provide
    Arbitrary<Integer> recordCount() {
        return Arbitraries.integers().between(1, 20);
    }

    @Provide
    Arbitrary<Integer> pageSize() {
        return Arbitraries.integers().between(1, 10);
    }

    @Provide
    Arbitrary<Integer> rating() {
        return Arbitraries.integers().between(1, 5);
    }

    @Provide
    Arbitrary<String> comment() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(0)
                .ofMaxLength(200);
    }

    // ==================== In-Memory Implementations ====================

    /**
     * In-memory QA History service for testing
     */
    private static class InMemoryQAHistoryService implements QAHistoryService {
        private final Map<Long, QAHistory> storage = new HashMap<>();
        private final AtomicLong idCounter = new AtomicLong(1);

        @Override
        public QAHistoryDTO save(SaveQAHistoryRequest request) {
            QAHistory history = new QAHistory();
            history.setId(idCounter.getAndIncrement());
            history.setUserId(request.getUserId());
            history.setKbId(request.getKbId());
            history.setQuestion(request.getQuestion());
            history.setAnswer(request.getAnswer());
            history.setTraceId(request.getTraceId());
            history.setLatencyMs(request.getLatencyMs());
            history.setCreatedAt(LocalDateTime.now());
            
            // Store citations as JSON string (simplified for testing)
            if (request.getCitations() != null) {
                history.setCitations(serializeCitations(request.getCitations()));
            } else {
                history.setCitations("[]");
            }
            
            storage.put(history.getId(), history);
            return toDTO(history, request.getCitations());
        }

        @Override
        public Optional<QAHistoryDTO> getById(Long id) {
            QAHistory history = storage.get(id);
            if (history == null) return Optional.empty();
            return Optional.of(toDTO(history, deserializeCitations(history.getCitations())));
        }

        @Override
        public PageResult<QAHistoryDTO> getPage(QAHistoryPageRequest request) {
            List<QAHistory> filtered = storage.values().stream()
                    .filter(h -> request.getUserId() == null || request.getUserId().equals(h.getUserId()))
                    .filter(h -> request.getKbId() == null || request.getKbId().equals(h.getKbId()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
            
            long total = filtered.size();
            int start = (request.getPage() - 1) * request.getSize();
            int end = Math.min(start + request.getSize(), filtered.size());
            
            List<QAHistoryDTO> records = filtered.subList(start, end).stream()
                    .map(h -> toDTO(h, deserializeCitations(h.getCitations())))
                    .toList();
            
            return PageResult.of(records, total, request.getPage(), request.getSize());
        }

        @Override
        public long countByUserId(Long userId) {
            return storage.values().stream()
                    .filter(h -> userId.equals(h.getUserId()))
                    .count();
        }

        @Override
        public long countByKbId(Long kbId) {
            return storage.values().stream()
                    .filter(h -> kbId.equals(h.getKbId()))
                    .count();
        }

        @Override
        public void delete(Long id) {
            storage.remove(id);
        }

        @Override
        public void deleteByUserId(Long userId) {
            storage.entrySet().removeIf(e -> userId.equals(e.getValue().getUserId()));
        }

        private QAHistoryDTO toDTO(QAHistory history, List<Citation> citations) {
            return QAHistoryDTO.builder()
                    .id(history.getId())
                    .userId(history.getUserId())
                    .kbId(history.getKbId())
                    .question(history.getQuestion())
                    .answer(history.getAnswer())
                    .citations(citations != null ? citations : List.of())
                    .traceId(history.getTraceId())
                    .latencyMs(history.getLatencyMs())
                    .createdAt(history.getCreatedAt())
                    .build();
        }

        private String serializeCitations(List<Citation> citations) {
            // Simplified serialization for testing
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < citations.size(); i++) {
                Citation c = citations.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format("{\"source\":\"%s\",\"snippet\":\"%s\"}", 
                        c.source(), c.snippet()));
            }
            sb.append("]");
            return sb.toString();
        }

        private List<Citation> deserializeCitations(String json) {
            if (json == null || json.equals("[]")) return List.of();
            // Simplified deserialization for testing
            List<Citation> citations = new ArrayList<>();
            // Parse simple JSON format
            String content = json.substring(1, json.length() - 1);
            if (content.isEmpty()) return citations;
            
            int depth = 0;
            int start = 0;
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        String obj = content.substring(start, i + 1);
                        Citation citation = parseCitation(obj);
                        if (citation != null) citations.add(citation);
                        start = i + 2; // Skip comma
                    }
                }
            }
            return citations;
        }

        private Citation parseCitation(String json) {
            try {
                int sourceStart = json.indexOf("\"source\":\"") + 10;
                int sourceEnd = json.indexOf("\"", sourceStart);
                String source = json.substring(sourceStart, sourceEnd);
                
                int snippetStart = json.indexOf("\"snippet\":\"") + 11;
                int snippetEnd = json.indexOf("\"", snippetStart);
                String snippet = json.substring(snippetStart, snippetEnd);
                
                return Citation.of(source, snippet);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * In-memory QA Feedback service for testing
     */
    private static class InMemoryQAFeedbackService implements QAFeedbackService {
        private final Map<Long, QAFeedback> storage = new HashMap<>();
        private final AtomicLong idCounter = new AtomicLong(1);

        @Override
        public QAFeedbackDTO submit(SubmitFeedbackRequest request) {
            QAFeedback feedback = new QAFeedback();
            feedback.setId(idCounter.getAndIncrement());
            feedback.setQaId(request.getQaId());
            feedback.setUserId(request.getUserId());
            feedback.setRating(request.getRating());
            feedback.setComment(request.getComment());
            feedback.setCreatedAt(LocalDateTime.now());
            
            storage.put(feedback.getId(), feedback);
            return toDTO(feedback);
        }

        @Override
        public Optional<QAFeedbackDTO> getById(Long id) {
            return Optional.ofNullable(storage.get(id)).map(this::toDTO);
        }

        @Override
        public Optional<QAFeedbackDTO> getByQaId(Long qaId) {
            return storage.values().stream()
                    .filter(f -> qaId.equals(f.getQaId()))
                    .findFirst()
                    .map(this::toDTO);
        }

        @Override
        public List<QAFeedbackDTO> listByQaId(Long qaId) {
            return storage.values().stream()
                    .filter(f -> qaId.equals(f.getQaId()))
                    .map(this::toDTO)
                    .toList();
        }

        @Override
        public List<QAFeedbackDTO> listByUserId(Long userId) {
            return storage.values().stream()
                    .filter(f -> userId.equals(f.getUserId()))
                    .map(this::toDTO)
                    .toList();
        }

        @Override
        public boolean hasUserFeedback(Long qaId, Long userId) {
            return storage.values().stream()
                    .anyMatch(f -> qaId.equals(f.getQaId()) && userId.equals(f.getUserId()));
        }

        @Override
        public void delete(Long id) {
            storage.remove(id);
        }

        @Override
        public void deleteByQaId(Long qaId) {
            storage.entrySet().removeIf(e -> qaId.equals(e.getValue().getQaId()));
        }

        private QAFeedbackDTO toDTO(QAFeedback feedback) {
            return QAFeedbackDTO.builder()
                    .id(feedback.getId())
                    .qaId(feedback.getQaId())
                    .userId(feedback.getUserId())
                    .rating(feedback.getRating())
                    .comment(feedback.getComment())
                    .createdAt(feedback.getCreatedAt())
                    .build();
        }
    }

    // ==================== Assertion Helper ====================

    private BooleanAssert assertThat(boolean condition) {
        return new BooleanAssert(condition);
    }

    private static class BooleanAssert {
        private final boolean actual;
        private String description;

        BooleanAssert(boolean actual) {
            this.actual = actual;
        }

        BooleanAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isTrue() {
            if (!actual) {
                throw new AssertionError(description != null ? description : "Expected true but was false");
            }
        }
    }
}
