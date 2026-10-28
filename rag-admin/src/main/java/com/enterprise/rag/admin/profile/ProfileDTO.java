package com.enterprise.rag.admin.profile;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
public record ProfileDTO(Long id, String username, String displayName, String email,
                         boolean hasAvatar, int revision) { }
