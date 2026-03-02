package com.enterprise.rag.admin.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.kb.entity.KBPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库权限 Mapper 接口
 */
@Mapper
public interface KBPermissionMapper extends BaseMapper<KBPermission> {
}
