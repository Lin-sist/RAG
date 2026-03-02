package com.enterprise.rag.auth.exception;

import com.enterprise.rag.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 认证异常
 */
public class AuthException extends BusinessException {

    public AuthException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.UNAUTHORIZED);
    }

    public AuthException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message, httpStatus);
    }

    public static AuthException invalidCredentials() {
        return new AuthException("AUTH_001", "用户名或密码错误");
    }

    public static AuthException tokenExpired() {
        return new AuthException("AUTH_002", "Token 已过期，请重新登录");
    }

    public static AuthException invalidToken() {
        return new AuthException("AUTH_003", "无效的 Token");
    }

    public static AuthException accessDenied() {
        return new AuthException("AUTH_004", "权限不足", HttpStatus.FORBIDDEN);
    }

    public static AuthException tokenBlacklisted() {
        return new AuthException("AUTH_005", "Token 已失效，请重新登录");
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException("AUTH_006", "无效的 Refresh Token");
    }

    public static AuthException userDisabled() {
        return new AuthException("AUTH_007", "用户已被禁用");
    }
}
