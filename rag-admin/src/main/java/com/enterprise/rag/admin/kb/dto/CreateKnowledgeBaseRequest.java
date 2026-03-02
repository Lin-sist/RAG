package com.enterprise.rag.admin.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建知识库请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateKnowledgeBaseRequest {
    
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称不能超过100个字符")
    private String name;
    
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;
    
    @Builder.Default
    private Boolean isPublic = false;
}
