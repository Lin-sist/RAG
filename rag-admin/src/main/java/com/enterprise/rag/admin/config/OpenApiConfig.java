package com.enterprise.rag.admin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 配置
 * 配置 JWT Bearer 认证方案，使 Swagger UI 显示 Authorize 按钮
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Token";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise RAG QA System API")
                        .description("企业内部 AI 驱动的技术文档与代码知识库问答系统")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Enterprise Team")))
                // 全局安全要求：所有接口默认需要 Bearer Token
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                // 定义安全方案：Bearer JWT
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请先调用 /auth/login 获取 token，然后在此输入（不需要加 Bearer 前缀）")));
    }
}
