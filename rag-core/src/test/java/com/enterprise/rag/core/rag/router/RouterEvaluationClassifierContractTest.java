package com.enterprise.rag.core.rag.router;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterEvaluationClassifierContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void frozenReleaseMatchesClassifierWithoutRuntimeLabelDependency() throws Exception {
        Path root = repositoryRoot();
        Map<String, String> questions = readDatasetQuestions(
                root.resolve("docs/eval/releases/rag-eval-dev-v2.jsonl"));
        List<String> expectationLines = Files.readAllLines(root.resolve(
                "docs/eval/router/bounded-query-router-eval-v1-expectations.jsonl"));
        DeterministicFactIntentClassifier classifier = new DeterministicFactIntentClassifier();
        Map<QueryIntent, Integer> distribution = new LinkedHashMap<>();

        for (String line : expectationLines) {
            if (line.isBlank()) {
                continue;
            }
            JsonNode expectation = objectMapper.readTree(line);
            String sampleId = expectation.path("sampleId").asText();
            String question = questions.get(sampleId);
            assertTrue(question != null && !question.isBlank(), sampleId);
            QueryClassification first = classifier.classify(question);
            QueryClassification second = classifier.classify(question);
            assertEquals(first, second, sampleId);
            assertEquals(expectation.path("expectedIntent").asText(), first.intent().name(), sampleId);
            assertEquals("REQUIRED", expectation.path("requiredStatus").asText(), sampleId);
            distribution.merge(first.intent(), 1, Integer::sum);
        }

        assertEquals(Map.of(QueryIntent.FACT, 10, QueryIntent.UNSUPPORTED, 10), distribution);
        assertEquals(0L, java.util.Arrays.stream(
                        DeterministicFactIntentClassifier.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .count());
    }

    @Test
    void productionClassifierSourceDoesNotReferenceEvaluationArtifactsOrSampleLabels() throws Exception {
        Path source = repositoryRoot().resolve(
                "rag-core/src/main/java/com/enterprise/rag/core/rag/router/DeterministicFactIntentClassifier.java");
        String content = Files.readString(source);

        assertFalse(content.contains("docs/eval"));
        assertFalse(content.contains("expectation"));
        assertFalse(content.contains("sampleId"));
        assertFalse(content.contains("definition-001"));
    }

    private Map<String, String> readDatasetQuestions(Path dataset) throws IOException {
        Map<String, String> questions = new LinkedHashMap<>();
        for (String line : Files.readAllLines(dataset)) {
            if (!line.isBlank()) {
                JsonNode sample = objectMapper.readTree(line);
                questions.put(sample.path("id").asText(), sample.path("question").asText());
            }
        }
        return questions;
    }

    private Path repositoryRoot() {
        String multiModuleRoot = System.getProperty("maven.multiModuleProjectDirectory");
        if (multiModuleRoot != null) {
            Path candidate = Path.of(multiModuleRoot).toAbsolutePath().normalize();
            if (Files.isRegularFile(candidate.resolve("docs/eval/releases/rag-eval-dev-v2.jsonl"))) {
                return candidate;
            }
        }
        Path candidate = Path.of("").toAbsolutePath().normalize();
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("docs/eval/releases/rag-eval-dev-v2.jsonl"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Repository root unavailable");
    }
}
