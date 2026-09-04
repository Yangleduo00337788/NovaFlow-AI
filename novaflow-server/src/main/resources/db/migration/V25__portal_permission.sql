-- Portal 用户前台权限
INSERT INTO permission (permission_code, permission_name, module) VALUES
('portal:access', '访问用户前台', 'portal')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 普通用户：Portal + Agent 对话
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code IN ('portal:access', 'agent:chat')
WHERE r.tenant_id = 0 AND r.role_code = 'user';

-- 企业/平台管理员：随全量权限一并授予
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code = 'portal:access'
WHERE r.tenant_id = 0 AND r.role_code IN ('tenant_admin', 'super_admin');

-- 开发者：补充 portal:access（其余权限已在 V11 种子中）
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code = 'portal:access'
WHERE r.tenant_id = 0 AND r.role_code = 'developer';
