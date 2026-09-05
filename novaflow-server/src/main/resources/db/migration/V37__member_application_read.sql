-- Phase 12：Member 补 application:read，与门户资源 ACL 及 §九 矩阵对齐

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code = 'application:read'
WHERE r.tenant_id = 0 AND r.role_code = 'member';
