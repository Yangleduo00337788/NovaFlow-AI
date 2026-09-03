-- RBAC v2 Phase 5：权限矩阵对齐（§九）
-- Developer 无调用日志；Viewer 无 Trace / 调用日志

DELETE rp FROM role_permission rp
INNER JOIN role r ON r.id = rp.role_id AND r.tenant_id = 0 AND r.role_code = 'developer'
INNER JOIN permission p ON p.id = rp.permission_id AND p.permission_code = 'log:read';

DELETE rp FROM role_permission rp
INNER JOIN role r ON r.id = rp.role_id AND r.tenant_id = 0 AND r.role_code = 'viewer'
INNER JOIN permission p ON p.id = rp.permission_id AND p.permission_code IN ('trace:view', 'log:read');
