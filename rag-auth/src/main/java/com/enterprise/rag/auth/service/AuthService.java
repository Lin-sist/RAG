package com.enterprise.rag.auth.service;

import com.enterprise.rag.auth.dto.AuthResponse;
import com.enterprise.rag.auth.dto.LoginRequest;
import com.enterprise.rag.auth.model.UserPrincipal;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 认证响应（包含 Token）
     */
    AuthResponse login(LoginRequest request);

    /**
     * 用户登出
     * 
     * @param accessToken Access Token
     */
    void logout(String accessToken);

    /**
     * 刷新 Token
     * 
     * @param refreshToken Refresh Token
     * @return 新的认证响应
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * 验证 Token 并获取用户信息
     * 
     * @param token JWT Token
     * @return 用户主体信息
     */
    UserPrincipal validateToken(String token);
}
