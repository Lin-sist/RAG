package com.enterprise.rag.admin.controller;

import com.enterprise.rag.auth.dto.AuthResponse;
import com.enterprise.rag.auth.dto.LoginRequest;
import com.enterprise.rag.auth.dto.RefreshTokenRequest;
import com.enterprise.rag.auth.service.AuthService;
import com.enterprise.rag.common.model.ApiResponse;
import com.enterprise.rag.common.ratelimit.RateLimit;
import com.enterprise.rag.common.ratelimit.RateLimitDimension;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 API 控制器
 * <p>
 * 提供用户登录、登出、Token 刷新等认证相关接口。
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证相关接口：登录、登出、Token 刷新")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 认证响应（包含 Token）
     */
    @PostMapping("/login")
    @RateLimit(maxRequests = 20, windowSeconds = 60, dimension = RateLimitDimension.IP, message = "登录请求过于频繁，请稍后重试")
    @Operation(summary = "用户登录", description = "使用用户名和密码进行登录，返回 JWT Token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());
        AuthResponse response = authService.login(request);
        log.info("用户登录成功: username={}", request.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 用户登出
     *
     * @param authorization Authorization 请求头
     * @return 空响应
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "使当前 Token 失效，加入黑名单")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登出成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token 无效或已过期")
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(description = "Bearer Token", required = true) @RequestHeader("Authorization") String authorization) {
        String token = extractToken(authorization);
        log.info("用户登出请求");
        authService.logout(token);
        log.info("用户登出成功");
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 刷新 Token
     *
     * @param request 刷新 Token 请求
     * @return 新的认证响应
     */
    @PostMapping("/refresh")
    @RateLimit(maxRequests = 60, windowSeconds = 60, dimension = RateLimitDimension.IP, message = "刷新请求过于频繁，请稍后重试")
    @Operation(summary = "刷新 Token", description = "使用 Refresh Token 获取新的 Access Token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "刷新成功", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token 无效或已过期")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token 刷新请求");
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        log.info("Token 刷新成功");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 从 Authorization 头中提取 Token
     *
     * @param authorization Authorization 头
     * @return Token 字符串
     */
    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
