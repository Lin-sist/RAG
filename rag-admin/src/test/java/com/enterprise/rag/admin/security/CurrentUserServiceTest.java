package com.enterprise.rag.admin.security;

import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentUserServiceTest {

    private final CurrentUserService currentUserService = new CurrentUserService();

    @Test
    void shouldReturnIdWhenPrincipalIsUserPrincipal() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(42L)
                .username("alice")
                .password("secret")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        Long userId = currentUserService.requireUserId(principal);

        assertEquals(42L, userId);
    }

    @Test
    void shouldThrowWhenPrincipalIsNull() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> currentUserService.requireUserId(null));

        assertEquals("AUTH_001", exception.getErrorCode());
    }

    @Test
    void shouldThrowWhenPrincipalIsNotUserPrincipal() {
        UserDetails springUser = User.withUsername("alice")
                .password("secret")
                .roles("USER")
                .build();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> currentUserService.requireUserId(springUser));

        assertEquals("AUTH_001", exception.getErrorCode());
    }

    @Test
    void shouldThrowWhenPrincipalIdIsNull() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(null)
                .username("alice")
                .password("secret")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> currentUserService.requireUserId(principal));

        assertEquals("AUTH_001", exception.getErrorCode());
    }
}
