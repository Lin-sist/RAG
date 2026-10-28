package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.profile.*;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@RestController
@RequestMapping("/api/account/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;
    private final ObjectMapper json;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileDTO>> current(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.current(principal)));
    }
    @GetMapping("/avatar")
    public ResponseEntity<byte[]> avatar(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.IMAGE_PNG).body(service.avatar(principal));
    }
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileDTO>> save(@AuthenticationPrincipal UserDetails principal,
                                                       HttpServletRequest request) throws Exception {
        // Inspect the original parts: getFile(name) would hide duplicate files/unknown fields.
        var parts = request.getParts();
        if (parts.stream().anyMatch(part -> !Set.of("profile", "avatar").contains(part.getName()))
                || parts.stream().filter(part -> part.getName().equals("profile")).count() != 1
                || parts.stream().filter(part -> part.getName().equals("avatar")).count() > 1) throw ProfileInput.invalid();
        var metadata = request.getPart("profile");
        if (metadata.getSize() > 4096 || !MediaType.APPLICATION_JSON_VALUE.equals(metadata.getContentType())) throw ProfileInput.invalid();
        ProfileInput input;
        try (var stream = metadata.getInputStream()) {
            input = ProfileInput.parse(json, new String(stream.readNBytes(4097), StandardCharsets.UTF_8));
        }
        var file = request.getPart("avatar");
        byte[] bytes = null;
        if (file != null) {
            if (file.getSize() > AvatarNormalizer.SOURCE_LIMIT) throw new BusinessException("PROFILE_AVATAR_TOO_LARGE", "头像不能超过 2 MB", HttpStatus.PAYLOAD_TOO_LARGE);
            try (var stream = file.getInputStream()) { bytes = stream.readNBytes(AvatarNormalizer.SOURCE_LIMIT + 1); }
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.save(principal, input, bytes)));
    }
}
