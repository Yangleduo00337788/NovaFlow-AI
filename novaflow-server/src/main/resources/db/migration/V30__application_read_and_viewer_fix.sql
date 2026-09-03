-- RBAC v2 Phase 3：应用只读权限 + 修正 Viewer 应用管理越权

INSERT INTO permission (permission_code, permission_name, module) VALUES
('application:read', '查看应用', 'application')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- Owner / Admin / Developer / Operator：补充只读（便于资源 ACL 与菜单）
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code = 'application:read'
WHERE r.tenant_id = 0 AND r.role_code IN ('tenant_owner', 'tenant_admin', 'developer', 'operator', 'super_admin');

-- Viewer：只读应用，移除误授的 application:manage
DELETE rp FROM role_permission rp
INNER JOIN role r ON r.id = rp.role_id AND r.tenant_id = 0 AND r.role_code = 'viewer'
INNER JOIN permission p ON p.id = rp.permission_id AND p.permission_code = 'application:manage';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code = 'application:read'
WHERE r.tenant_id = 0 AND r.role_code = 'viewer';
