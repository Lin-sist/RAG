package com.enterprise.rag.auth.service.impl;

import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.auth.persistence.repository.AuthUserAccount;
import com.enterprise.rag.auth.persistence.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 从数据库加载认证用户及角色。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUserAccount user = authUserRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("用户不存在");
                    return new UsernameNotFoundException("用户不存在");
                });

        return UserPrincipal.builder()
                .id(user.id())
                .username(user.username())
                .password(user.passwordHash())
                .email(user.email())
                .enabled(user.enabled())
                .roles(user.roles())
                .build();
    }
}
