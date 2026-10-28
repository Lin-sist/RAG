package com.enterprise.rag.admin.session;

import com.enterprise.rag.admin.session.mapper.SessionContextMapper;

import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SessionContextService {
    private final CurrentUserService currentUserService;
    private final SessionContextMapper mapper;

    public SessionContextDTO current(UserDetails principal) {
        RequestIdentity identity = currentUserService.requireIdentity(principal);
        SessionContextMapper.ContextRow row = mapper.findCurrent(identity.userId(), identity.tenantId());
        if (row == null || !Objects.equals(identity.userId(), row.getUserId())
                || !Objects.equals(identity.tenantId(), row.getWorkspaceId())) {
            throw new BusinessException("AUTH_001", "未认证，请先登录", HttpStatus.UNAUTHORIZED);
        }
        if (row.getDisplayName() == null || row.getDisplayName().isBlank()) {
            throw new BusinessException("SESSION_CONTEXT_UNAVAILABLE", "工作空间资料暂不可用",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return new SessionContextDTO(
                new SessionContextDTO.UserInfo(row.getUserId(), row.getUsername(), row.getEmail(),
                        row.getProfileDisplayName(), row.isHasAvatar(), row.getProfileRevision()),
                new SessionContextDTO.Workspace(row.getWorkspaceId().toString(), row.getDisplayName()));
    }
}
