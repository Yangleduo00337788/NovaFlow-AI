-- RBAC v2 Phase 8：§九 账单查看权限对齐（Developer/Operator 无账单菜单）

DELETE rp FROM role_permission rp
INNER JOIN role r ON r.id = rp.role_id AND r.tenant_id = 0 AND r.role_code IN ('developer', 'operator')
INNER JOIN permission p ON p.id = rp.permission_id AND p.permission_code = 'billing:view';
