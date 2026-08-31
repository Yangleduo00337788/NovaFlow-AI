-- V1.0 平台超管、审计日志、全局搜索权限
INSERT INTO permission (permission_code, permission_name, module) VALUES
('platform:manage', '平台超管', 'platform'),
('audit:view', '查看审计日志', 'audit'),
('search:global', '全局搜索', 'system')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.tenant_id = 0 AND r.role_code = 'super_admin'
  AND p.permission_code IN ('platform:manage', 'audit:view', 'search:global');

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code IN ('audit:view', 'search:global')
WHERE r.tenant_id = 0 AND r.role_code = 'tenant_admin';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code = 'search:global'
WHERE r.tenant_id = 0 AND r.role_code = 'developer';
