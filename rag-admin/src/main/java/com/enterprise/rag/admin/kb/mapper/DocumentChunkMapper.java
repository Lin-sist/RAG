package com.enterprise.rag.admin.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档分块 Mapper 接口
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {
}
