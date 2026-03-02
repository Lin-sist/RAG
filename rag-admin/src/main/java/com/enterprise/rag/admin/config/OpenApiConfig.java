package com.enterprise.rag.admin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger 配置                 copilot:2026-02-14
 * 
 * 作用：
 * 1. 配置 Swagger UI 的基本信息（标题、描述、版本等）
 * 2. 配置 JWT Bearer Token 认证，使 Swagger UI 显示 "Authorize" 按钮
 * 3. 设置全局安全要求，所有接口默认需要认证（除了 SecurityConfig 中排除的）
 * 
 * 面试点：
 * Q: 为什么需要在 Swagger 中配置 SecurityScheme？
 * A:
 * - Swagger UI 需要知道 API 使用哪种认证方式（Basic、Bearer、ApiKey 等）
 * - 配置后，用户可以在 Swagger UI 中点击 "Authorize" 按钮输入 Token
 * - Token 会自动添加到后续所有请求的 Authorization Header 中
 * 
 * Q: SecurityScheme.Type.HTTP 和 Scheme "bearer" 的含义？
 * A:
 * - Type.HTTP 表示使用 HTTP 认证标准（RFC 7235）
 * - "bearer" 表示使用 Bearer Token 格式（RFC 6750）
 * - 最终请求头格式：Authorization: Bearer <token>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    /**
     * 配置 OpenAPI 文档
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // API 基本信息
                .info(new Info()
                        .title("RAG 知识库问答系统 API")
                        .description("基于检索增强生成（RAG）的智能问答系统")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("RAG Team")
                                .email("[email protected]"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))

                // 配置安全认证组件
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                // 认证类型：HTTP 标准认证
                                .type(SecurityScheme.Type.HTTP)
                                // 认证方案：Bearer Token（JWT）
                                .scheme("bearer")
                                // Token 格式提示（可选，帮助文档说明）
                                .bearerFormat("JWT")
                                // 描述信息（会显示在 Swagger UI 的认证弹窗中）
                                .description("请输入 JWT Token（无需手动添加 'Bearer ' 前缀）")))

                // 全局安全要求：所有接口默认需要认证
                // 注意：SecurityConfig 中配置了 permitAll() 的接口不受此限制
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
