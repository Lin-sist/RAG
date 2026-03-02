package com.enterprise.rag.auth.filter;

import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.auth.provider.JwtTokenProvider;
import com.enterprise.rag.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 从请求头中提取 JWT Token 并验证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // 验证 Token
                if (!jwtTokenProvider.isTokenValid(token)) {
                    log.warn("JWT Token 无效");
                    filterChain.doFilter(request, response);
                    return;
                }

                // 检查黑名单
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.warn("JWT Token 已在黑名单中");
                    filterChain.doFilter(request, response);
                    return;
                }

                // 检查 Token 类型
                String tokenType = jwtTokenProvider.getTokenType(token);
                if (!"access".equals(tokenType)) {
                    log.warn("非 Access Token 类型");
                    filterChain.doFilter(request, response);
                    return;
                }

                UserPrincipal userPrincipal = jwtTokenProvider.getUserPrincipalFromToken(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userPrincipal,
                                null,
                                userPrincipal.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("用户 {} 认证成功", userPrincipal.getUsername());
            }
        } catch (Exception e) {
            log.error("JWT 认证过程中发生错误", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
}
