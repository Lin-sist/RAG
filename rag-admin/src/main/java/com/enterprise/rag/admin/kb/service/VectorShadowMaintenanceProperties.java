package com.enterprise.rag.admin.kb.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Default-off gate for non-REST vector maintenance. */
@Data
@Component
@ConfigurationProperties(prefix = "rag.vector-maintenance")
public class VectorShadowMaintenanceProperties {
    private boolean enabled = false;
}
