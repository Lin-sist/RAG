package com.enterprise.rag.admin.mcp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Property descriptions remain available without registering the disabled transport. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(McpProperties.class)
public class McpIntegrationConfiguration { }
