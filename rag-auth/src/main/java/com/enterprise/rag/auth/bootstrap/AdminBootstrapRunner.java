package com.enterprise.rag.auth.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapService adminBootstrapService;

    @Override
    public void run(ApplicationArguments args) {
        BootstrapOutcome outcome = adminBootstrapService.bootstrap();
        if (outcome != BootstrapOutcome.DISABLED) {
            log.info("Admin bootstrap completed: outcome={}", outcome);
        }
    }
}
