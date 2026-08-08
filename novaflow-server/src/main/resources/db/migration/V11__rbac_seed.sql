-- 系统权限
INSERT INTO permission (permission_code, permission_name, module) VALUES
('agent:create', '创建Agent', 'agent'),
('agent:edit', '编辑Agent', 'agent'),
('agent:delete', '删除Agent', 'agent'),
('agent:publish', '发布Agent', 'agent'),
('agent:chat', '使用Agent对话', 'agent'),
('workflow:create', '创建工作流', 'workflow'),
('workflow:edit', '编辑工作流', 'workflow'),
('knowledge:create', '创建知识库', 'knowledge'),
('knowledge:upload', '上传文档', 'knowledge'),
('model:config', '配置模型', 'model'),
('prompt:create', '创建Prompt模板', 'prompt'),
('prompt:edit', '编辑Prompt模板', 'prompt'),
('application:manage', '管理应用', 'application'),
('billing:view', '查看账单与用量', 'billing'),
('billing:manage', '管理配额预警', 'billing'),
('monitor:view', '查看运行监控', 'monitor'),
('trace:view', '查看追踪分析', 'monitor'),
('tenant:manage', '管理租户', 'tenant'),
('member:manage', '管理成员', 'tenant')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 系统角色
INSERT INTO role (tenant_id, role_code, role_name, is_system, is_deleted) VALUES
(0, 'super_admin', '超级管理员', 1, 0),
(0, 'tenant_admin', '企业管理员', 1, 0),
(0, 'developer', '开发者', 1, 0),
(0, 'user', '普通用户', 1, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 企业管理员：全部权限
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.tenant_id = 0 AND r.role_code = 'tenant_admin';

-- 超级管理员：全部权限
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.tenant_id = 0 AND r.role_code = 'super_admin';

-- 开发者：AI 开发与监控，不含系统管理
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code IN (
    'agent:create', 'agent:edit', 'agent:delete', 'agent:publish', 'agent:chat',
    'workflow:create', 'workflow:edit',
    'knowledge:create', 'knowledge:upload',
    'model:config',
    'prompt:create', 'prompt:edit',
    'application:manage',
    'monitor:view', 'trace:view',
    'billing:view'
)
WHERE r.tenant_id = 0 AND r.role_code = 'developer';

-- 普通用户：仅使用 Agent 对话
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code = 'agent:chat'
WHERE r.tenant_id = 0 AND r.role_code = 'user';
