package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.mcp.McpIntegrationService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/mcp")
public class McpIntegrationController {
    private final McpIntegrationService service;

    public McpIntegrationController(McpIntegrationService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<byte[]> metadata(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON)
                .body(service.response(principal));
    }
}
