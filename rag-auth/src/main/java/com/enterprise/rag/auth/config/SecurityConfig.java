package com.enterprise.rag.auth.config;

import com.enterprise.rag.auth.filter.JwtAuthenticationFilter;
import com.enterprise.rag.auth.handler.JwtAccessDeniedHandler;
import com.enterprise.rag.auth.handler.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
        private final UserDetailsService userDetailsService;

        /**
         * 密码编码器
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * 认证提供者
         */
        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        /**
         * 认证管理器
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        /**
         * 安全过滤链配置
         */
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // 禁用 CSRF（使用 JWT 无需 CSRF）
                                .csrf(AbstractHttpConfigurer::disable)

                                // 配置 CORS
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // 配置会话管理（无状态）
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // 配置异常处理
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                                .accessDeniedHandler(jwtAccessDeniedHandler))

                                // 配置请求授权
                                .authorizeHttpRequests(auth -> auth
                                                // 公开接口
                                                .requestMatchers(
                                                                "/auth/login",
                                                                "/auth/refresh",
                                                                "/actuator/health",
                                                                "/swagger-ui.html",             //copilot:2026-02-14
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/api-docs/**",
                                                                "/swagger-resources/**",
                                                                "/webjars/**",
                                                                "/error")
                                                .permitAll()
                                                // 管理员接口
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                // 其他接口需要认证
                                                .anyRequest().authenticated())

                                // 配置认证提供者
                                .authenticationProvider(authenticationProvider())

                                // 添加 JWT 过滤器
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /**
         * CORS 配置
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // 允许的源
                configuration.setAllowedOriginPatterns(List.of("*"));

                // 允许的方法
                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

                // 允许的头
                configuration.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "X-Requested-With",
                                "X-Trace-Id",
                                "X-Idempotency-Key"));

                // 暴露的头
                configuration.setExposedHeaders(Arrays.asList(
                                "X-Trace-Id",
                                "X-RateLimit-Remaining",
                                "X-RateLimit-Reset"));

                // 允许携带凭证
                configuration.setAllowCredentials(true);

                // 预检请求缓存时间
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
