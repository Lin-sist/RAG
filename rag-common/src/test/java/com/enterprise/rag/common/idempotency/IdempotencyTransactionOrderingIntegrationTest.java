package com.enterprise.rag.common.idempotency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotencyTransactionOrderingIntegrationTest {

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void idempotencyResultWriteShouldRunAfterBusinessTransactionCompletes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Idempotency-Key", "request-key");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestConfig.class)) {
            TransactionalService service = context.getBean(TransactionalService.class);
            RecordingIdempotencyHandler handler = context.getBean(RecordingIdempotencyHandler.class);

            assertTrue(AopUtils.isAopProxy(service));
            service.create();

            assertTrue(handler.transactionActiveInsideOperation);
            assertFalse(handler.transactionActiveAfterOperation,
                    "幂等 completed 写入必须位于业务事务提交之后");
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        RecordingIdempotencyHandler idempotencyHandler() {
            return new RecordingIdempotencyHandler();
        }

        @Bean
        IdempotencyAspect idempotencyAspect(RecordingIdempotencyHandler handler) {
            return new IdempotencyAspect(handler);
        }

        @Bean
        TransactionalService transactionalService() {
            return new TransactionalService();
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                    // 测试只观察 Spring 事务边界，不连接真实数据库。
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                    // no-op
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                    // no-op
                }
            };
        }
    }

    static class TransactionalService {

        @Transactional
        @Idempotent(keyPrefix = "test:create")
        public String create() {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return "created";
        }
    }

    static class RecordingIdempotencyHandler implements IdempotencyHandler {

        private boolean transactionActiveInsideOperation;
        private boolean transactionActiveAfterOperation;

        @Override
        public <T> IdempotencyResult<T> execute(
                String idempotencyKey, Supplier<T> operation, Class<T> resultType) {
            return execute(idempotencyKey, operation, resultType, 60);
        }

        @Override
        public <T> IdempotencyResult<T> execute(
                String idempotencyKey,
                Supplier<T> operation,
                Class<T> resultType,
                long ttlSeconds) {
            T result = operation.get();
            transactionActiveInsideOperation = true;
            transactionActiveAfterOperation = TransactionSynchronizationManager.isActualTransactionActive();
            return IdempotencyResult.newRequest(result);
        }

        @Override
        public boolean exists(String idempotencyKey) {
            return false;
        }

        @Override
        public <T> IdempotencyResult<T> getStoredResult(String idempotencyKey, Class<T> resultType) {
            return null;
        }

        @Override
        public void remove(String idempotencyKey) {
            // no-op
        }
    }
}
