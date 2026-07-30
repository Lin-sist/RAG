package com.enterprise.rag.integration;

import com.enterprise.rag.core.rag.generator.AnswerGenerator;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@TestConfiguration(proxyBeanMethods = false)
class DeterministicGenerationTestConfig {

    @Bean
    @Primary
    DeterministicAnswerGenerator deterministicAnswerGenerator() {
        return new DeterministicAnswerGenerator();
    }

    static final class DeterministicAnswerGenerator implements AnswerGenerator {

        private final AtomicInteger invocationCount = new AtomicInteger();

        @Override
        public GeneratedAnswer generate(String query, List<RetrievedContext> contexts) {
            invocationCount.incrementAndGet();
            if ("c15-deterministic-generation-error".equals(query)) {
                throw new IllegalStateException("deterministic generation failure");
            }
            return GeneratedAnswer.of(
                    "c14 deterministic synthetic answer",
                    List.of(),
                    Map.of("model", getModelName()));
        }

        @Override
        public Flux<String> generateStream(String query, List<RetrievedContext> contexts) {
            invocationCount.incrementAndGet();
            return Flux.just("c14 deterministic ", "synthetic answer");
        }

        @Override
        public String getModelName() {
            return "c14-deterministic-test";
        }

        int invocationCount() {
            return invocationCount.get();
        }
    }
}
