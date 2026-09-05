package com.enterprise.rag.admin.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseDTO {
    
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String vectorCollection;
    private String vectorProviderFamily;
    private String vectorModel;
    private String vectorEndpointIdentity;
    private String vectorRequestContract;
    private Integer vectorDimension;
    private String vectorGeneration;
    private String vectorIdentityFingerprint;
    private Integer documentCount;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
