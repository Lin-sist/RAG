-- Initial data
-- Version: 1.0.0

-- Insert default roles
INSERT INTO `role` (`name`, `description`) VALUES 
('ADMIN', '系统管理员'),
('USER', '普通用户'),
('KB_ADMIN', '知识库管理员');

-- Insert default permissions
INSERT INTO `permission` (`code`, `name`, `resource`, `action`) VALUES 
('kb:create', '创建知识库', 'knowledge_base', 'create'),
('kb:read', '查看知识库', 'knowledge_base', 'read'),
('kb:update', '更新知识库', 'knowledge_base', 'update'),
('kb:delete', '删除知识库', 'knowledge_base', 'delete'),
('doc:upload', '上传文档', 'document', 'create'),
('doc:read', '查看文档', 'document', 'read'),
('doc:delete', '删除文档', 'document', 'delete'),
('qa:ask', '问答查询', 'qa', 'read'),
('qa:history', '查看历史', 'qa_history', 'read'),
('user:manage', '用户管理', 'user', 'manage');

-- Assign permissions to roles
-- ADMIN gets all permissions
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `role` r, `permission` p WHERE r.name = 'ADMIN';

-- USER gets basic permissions
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `role` r, `permission` p 
WHERE r.name = 'USER' AND p.code IN ('kb:read', 'doc:read', 'qa:ask', 'qa:history');

-- KB_ADMIN gets knowledge base management permissions
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `role` r, `permission` p 
WHERE r.name = 'KB_ADMIN' AND p.code IN ('kb:create', 'kb:read', 'kb:update', 'kb:delete', 'doc:upload', 'doc:read', 'doc:delete', 'qa:ask', 'qa:history');

-- Insert default admin user (password: admin123)
-- Password hash is BCrypt encoded
INSERT INTO `user` (`username`, `password_hash`, `email`, `enabled`) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '[email]', 1);

-- Assign ADMIN role to admin user
INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id FROM `user` u, `role` r WHERE u.username = 'admin' AND r.name = 'ADMIN';
