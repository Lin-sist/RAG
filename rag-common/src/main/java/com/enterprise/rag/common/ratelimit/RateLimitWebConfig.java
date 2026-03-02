package com.enterprise.rag.common.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 限流 Web 配置
 * <p>
 * 注册限流拦截器到 Spring MVC
 */
@Configuration
@RequiredArgsConstructor
public class RateLimitWebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/error",
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                );
    }
}
