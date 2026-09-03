-- RBAC v2 Phase 7：应用发布权限拆分（Operator 可发布不可管理）

INSERT INTO permission (permission_code, permission_name, module) VALUES
('application:publish', '发布应用', 'application')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code = 'application:publish'
WHERE r.tenant_id = 0 AND r.role_code IN ('tenant_owner', 'tenant_admin', 'developer', 'operator', 'super_admin');

-- 运维：保留发布/下线，收回应用全量管理
DELETE rp FROM role_permission rp
INNER JOIN role r ON r.id = rp.role_id AND r.tenant_id = 0 AND r.role_code = 'operator'
INNER JOIN permission p ON p.id = rp.permission_id AND p.permission_code = 'application:manage';
