package com.enterprise.rag.auth.service.impl;

import com.enterprise.rag.auth.model.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户详情服务实现
 * 
 * 注意：这是一个临时实现，使用内存存储用户数据
 * 在实际项目中，应该从数据库加载用户信息
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    
    // 临时用户存储（实际项目中应使用数据库）
    private final Map<String, UserPrincipal> users = new ConcurrentHashMap<>();

    public UserDetailsServiceImpl(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 初始化默认用户（仅用于开发测试）
     * 使用 @EventListener(ApplicationReadyEvent.class) 确保在应用完全启动后执行，避免循环依赖
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initDefaultUsers() {
        // 管理员用户
        users.put("admin", UserPrincipal.builder()
                .id(1L)
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@enterprise.com")
                .enabled(true)
                .roles(Set.of("ADMIN", "USER"))
                .build());

        // 普通用户
        users.put("user", UserPrincipal.builder()
                .id(2L)
                .username("user")
                .password(passwordEncoder.encode("user123"))
                .email("user@enterprise.com")
                .enabled(true)
                .roles(Set.of("USER"))
                .build());

        log.info("已初始化默认用户: admin, user");
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserPrincipal user = users.get(username);
        
        if (user == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 返回用户副本，避免修改原始数据
        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .roles(user.getRoles())
                .build();
    }

    /**
     * 添加用户（用于测试）
     */
    public void addUser(UserPrincipal user) {
        users.put(user.getUsername(), user);
    }

    /**
     * 检查用户是否存在
     */
    public boolean userExists(String username) {
        return users.containsKey(username);
    }
}
