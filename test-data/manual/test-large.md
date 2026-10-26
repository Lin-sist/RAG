# JWT 登录模块完整架构设计文档

## 一、总体概述

JWT（JSON Web Token）是一种轻量级的身份认证机制。与传统的 Session-Cookie 认证方案不同，JWT 将用户身份信息编码在 Token 中，服务器无需维护会话状态，天然适合分布式和微服务架构。

本文档详细阐述了 RAG 知识库问答系统中 JWT 登录认证模块的架构设计，包括认证流程、Token 管理策略、安全防护机制以及异常处理方案。

## 二、核心架构设计

### 2.1 双 Token 机制

系统采用 AccessToken + RefreshToken 的双 Token 机制：

- **AccessToken（访问令牌）**：短期有效（默认1小时），用于 API 请求认证
- **RefreshToken（刷新令牌）**：长期有效（默认7天），用于在 AccessToken 过期后获取新的 AccessToken

这种设计兼顾了 **安全性** 和 **用户体验**：
- 安全性：即使 AccessToken 被窃取，攻击窗口也仅限于其有效期
- 用户体验：RefreshToken 避免了用户频繁重新登录的麻烦

### 2.2 认证流程详解

#### 登录流程
1. 客户端发送用户名和密码到 `/auth/login` 端点
2. 服务器通过 Spring Security 的 `AuthenticationManager` 验证凭据
3. 凭据正确则生成 AccessToken 和 RefreshToken
4. 返回 Token 对和用户基本信息给客户端
5. 客户端将 Token 存储在 localStorage/sessionStorage 中

#### Token 刷新流程
1. 客户端检测到 AccessToken 过期（收到 401 响应）
2. 使用 RefreshToken 请求 `/auth/refresh` 端点
3. 服务器验证 RefreshToken 有效性
4. 生成新的 AccessToken（可选：同时轮换 RefreshToken）
5. 返回新的 Token 对

#### 登出流程
1. 客户端请求 `/auth/logout`
2. 服务器将当前 AccessToken 加入 Redis 黑名单
3. 黑名单 TTL 设置为 Token 的剩余有效期
4. 客户端清除本地存储的 Token

### 2.3 Spring Security 过滤器链

系统使用自定义的 `JwtAuthenticationFilter` 插入到 Spring Security 过滤器链中：

`java
// SecurityConfig.java
http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
`

过滤器的处理逻辑：
1. 从 `Authorization` Header 中提取 Bearer Token
2. 检查 Token 是否在 Redis 黑名单中
3. 解析 Token 获取用户信息（username, roles）
4. 创建 `Authentication` 对象放入 `SecurityContextHolder`
5. 请求通过后续过滤器链

## 三、安全防护机制

### 3.1 Token 黑名单

使用 Redis 的 String 数据结构实现 Token 黑名单：

- Key: `token:blacklist:{tokenHash}`
- Value: 用户 ID
- TTL: Token 剩余有效期

这样在 Token 尚未自然过期前，登出操作能立即使 Token 失效。

### 3.2 防重放攻击

每个 Token 包含唯一的 `jti`（JWT ID），结合 Redis 可以检测和阻止 Token 重放。

### 3.3 密码安全

- 使用 BCrypt 算法进行密码哈希
- BCrypt 自带盐值，每次哈希结果不同
- 工作因子（cost factor）设置为 10

### 3.4 接口安全

- 所有 `/api/**` 接口需要认证
- `/auth/**` 接口（登录、注册、刷新）公开访问
- CORS 配置允许前端开发时跨域请求
- Rate Limiting 限制登录接口调用频率

## 四、异常处理

### 4.1 Token 相关异常

| 异常类型 | HTTP 状态码 | 错误码 | 说明 |
|---------|-----------|--------|------|
| Token 缺失 | 401 | AUTH_001 | 请求未携带 Token |
| Token 过期 | 401 | AUTH_002 | Token 已过期 |
| Token 无效 | 401 | AUTH_003 | Token 签名不正确 |
| Token 黑名单 | 401 | AUTH_004 | Token 已被登出失效 |
| 权限不足 | 403 | AUTH_005 | 用户无访问权限 |

### 4.2 全局异常处理器

系统使用 `@RestControllerAdvice` 统一处理异常，保证 API 返回格式一致：

`java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthError(AuthenticationException e) {
        return ResponseEntity.status(401)
            .body(ApiResponse.error("AUTH_001", e.getMessage()));
    }
}
`

## 五、总结

本模块通过 JWT 双 Token 机制 + Redis 黑名单 + Spring Security 过滤器链实现了安全且高效的认证方案。在分布式环境下无需共享 Session，适合微服务架构。
