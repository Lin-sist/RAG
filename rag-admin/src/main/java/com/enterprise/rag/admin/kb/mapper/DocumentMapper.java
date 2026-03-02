package com.enterprise.rag.admin.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.kb.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper 接口
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
