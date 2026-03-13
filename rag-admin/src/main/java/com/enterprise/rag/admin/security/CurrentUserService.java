package com.enterprise.rag.admin.security;

import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 统一获取当前登录用户信息，避免各控制器自行解析身份字段。
 */
@Component
public class CurrentUserService {

    public Long requireUserId(UserDetails userDetails) {
        if (!(userDetails instanceof UserPrincipal principal) || principal.getId() == null) {
            throw new BusinessException("AUTH_001", "未认证，请先登录", HttpStatus.UNAUTHORIZED);
        }
        return principal.getId();
    }
}