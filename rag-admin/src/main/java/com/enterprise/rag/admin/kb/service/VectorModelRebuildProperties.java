package com.enterprise.rag.admin.kb.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Default-off gate and frozen bounds for C17 model-space rebuilds. */
@Data
@Component
@ConfigurationProperties(prefix = "rag.vector-model-rebuild")
public class VectorModelRebuildProperties {
    private boolean enabled = false;
    private int expectedCount = 50;
    private int maxHttpRequests = 11;
}
