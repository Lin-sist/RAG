package com.enterprise.rag.admin.profile;

import com.enterprise.rag.admin.profile.mapper.ProfileMapper;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final CurrentUserService identities;
    private final ProfileMapper mapper;
    private final AvatarNormalizer normalizer;

    public ProfileDTO current(UserDetails principal) { return dto(require(identities.requireIdentity(principal))); }
    public byte[] avatar(UserDetails principal) {
        var identity = identities.requireIdentity(principal);
        require(identity);
        var row = mapper.avatar(identity.userId(), identity.tenantId());
        byte[] data = row == null ? null : row.getData();
        if (data == null) throw new BusinessException("PROFILE_AVATAR_NOT_FOUND", "尚未设置头像", HttpStatus.NOT_FOUND);
        return data;
    }

    @Transactional
    public ProfileDTO save(UserDetails principal, ProfileInput input, byte[] avatar) {
        var identity = identities.requireIdentity(principal);
        require(identity);
        if (input.avatarAction().equals("REPLACE") != (avatar != null)) throw ProfileInput.invalid();
        byte[] normalized = avatar == null ? null : normalizer.normalize(avatar);
        // MyBatis-Plus treats top-level array parameters as batches; wrap primitive bytes as a scalar value.
        var content = new ProfileMapper.AvatarRow();
        content.setData(normalized);
        try {
            if (mapper.save(identity.userId(), identity.tenantId(), input, content) != 1) {
                require(identity);
                throw new BusinessException("PROFILE_REVISION_CONFLICT", "资料已在其他位置更新，请重新读取后再保存", HttpStatus.CONFLICT);
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException("PROFILE_EMAIL_UNAVAILABLE", "此邮箱暂不可用，请更换邮箱", HttpStatus.CONFLICT);
        }
        return dto(require(identity));
    }

    private ProfileMapper.Row require(RequestIdentity identity) {
        var row = mapper.current(identity.userId(), identity.tenantId());
        if (row == null || !java.util.Objects.equals(identity.userId(), row.getId())) throw new BusinessException("AUTH_001", "未认证，请先登录", HttpStatus.UNAUTHORIZED);
        return row;
    }
    private ProfileDTO dto(ProfileMapper.Row row) {
        return new ProfileDTO(row.getId(), row.getUsername(), row.getDisplayName(), row.getEmail(), row.isHasAvatar(), row.getRevision());
    }
}
