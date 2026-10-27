package com.enterprise.rag.admin.session;

/** Current server-owned identity for read-only presentation. */
public record SessionContextDTO(UserInfo userInfo, Workspace workspace) {
    public record UserInfo(Long id, String username, String email) { }
    public record Workspace(String id, String displayName) { }
}
