package com.enterprise.rag.integration;

import com.enterprise.rag.core.embedding.EmbeddingProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@TestConfiguration(proxyBeanMethods = false)
class DeterministicEmbeddingTestConfig {

    @Bean
    DeterministicEmbeddingProvider deterministicEmbeddingProvider() {
        return new DeterministicEmbeddingProvider();
    }

    static final class DeterministicEmbeddingProvider implements EmbeddingProvider {

        static final int DIMENSION = 64;
        private static final int BUCKETS_PER_TOKEN = 4;

        private final AtomicInteger invocationCount = new AtomicInteger();

        @Override
        public float[] getEmbedding(String text) {
            invocationCount.incrementAndGet();

            String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
            String[] tokens = Arrays.stream(normalized.split("[^\\p{L}\\p{N}_]+"))
                    .filter(token -> !token.isBlank())
                    .toArray(String[]::new);
            if (tokens.length == 0) {
                tokens = new String[]{"empty"};
            }

            float[] vector = new float[DIMENSION];
            for (String token : tokens) {
                byte[] digest = sha256(token);
                for (int i = 0; i < BUCKETS_PER_TOKEN; i++) {
                    int bucket = Byte.toUnsignedInt(digest[i * 2]) % DIMENSION;
                    float sign = (digest[i * 2 + 1] & 1) == 0 ? 1.0f : -1.0f;
                    vector[bucket] += sign;
                }
            }

            double norm = 0.0d;
            for (float value : vector) {
                norm += value * value;
            }
            norm = Math.sqrt(norm);
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
            return vector;
        }

        @Override
        public int getDimension() {
            return DIMENSION;
        }

        @Override
        public String getModelName() {
            return "deterministic-test";
        }

        @Override
        public int getPriority() {
            return 0;
        }

        int invocationCount() {
            return invocationCount.get();
        }

        private static byte[] sha256(String token) {
            try {
                return MessageDigest.getInstance("SHA-256")
                        .digest(token.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 unavailable", e);
            }
        }
    }
}
