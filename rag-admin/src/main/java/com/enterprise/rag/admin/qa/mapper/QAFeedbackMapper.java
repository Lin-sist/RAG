package com.enterprise.rag.admin.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.qa.entity.QAFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问答反馈 Mapper 接口
 */
@Mapper
public interface QAFeedbackMapper extends BaseMapper<QAFeedback> {
}
