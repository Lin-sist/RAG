package com.enterprise.rag.admin.kb.task;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "document.index-task-reconciliation")
public class IndexTaskReconciliationProperties {

    private boolean enabled = true;
    private boolean resumeEnabled = false;
    private int batchSize = 20;
    private int concurrency = 1;
    private int leaseSeconds = 300;
    private int heartbeatSeconds = 60;
    private int maxAttempts = 3;
    private int initialBackoffSeconds = 30;
    private int maxBackoffSeconds = 1800;
    private long scanIntervalMs = 30_000L;
}
