package com.enterprise.rag.admin.session;

/** Current server-owned identity for read-only presentation. */
public record SessionContextDTO(UserInfo userInfo, Workspace workspace) {
    public record UserInfo(Long id, String username, String email, String displayName, boolean hasAvatar, Integer profileRevision) {
        public UserInfo(Long id, String username, String email) { this(id, username, email, null, false, null); }
    }
    public record Workspace(String id, String displayName) { }
}
