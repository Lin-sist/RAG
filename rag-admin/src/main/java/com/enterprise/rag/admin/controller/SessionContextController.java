package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.session.SessionContextDTO;
import com.enterprise.rag.admin.session.SessionContextService;
import com.enterprise.rag.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionContextController {
    private final SessionContextService service;

    @GetMapping("/context")
    @Operation(summary = "读取当前账号与工作空间", description = "仅使用服务端认证身份，不提供租户选择")
    public ResponseEntity<ApiResponse<SessionContextDTO>> context(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(service.current(principal)));
    }
}
