package com.enterprise.rag.admin.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.qa.entity.QAHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问答历史 Mapper 接口
 */
@Mapper
public interface QAHistoryMapper extends BaseMapper<QAHistory> {
}
