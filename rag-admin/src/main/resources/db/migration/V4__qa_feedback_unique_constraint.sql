-- FEEDBACK-01: 防止同一用户对同一问答重复提交反馈

-- 清理历史重复数据，保留最早一条记录
DELETE f1 FROM qa_feedback f1
INNER JOIN qa_feedback f2
    ON f1.qa_id = f2.qa_id
   AND f1.user_id = f2.user_id
   AND f1.id > f2.id;

-- 增加唯一约束，数据库层保证幂等
ALTER TABLE qa_feedback
    ADD CONSTRAINT uk_qa_feedback_qa_user UNIQUE (qa_id, user_id);
