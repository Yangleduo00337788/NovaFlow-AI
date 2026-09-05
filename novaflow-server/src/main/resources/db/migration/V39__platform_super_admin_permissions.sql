-- Phase 18: 收窄 super_admin 为平台域专用权限（platform:manage + audit:view）

DELETE rp
FROM role_permission rp
         INNER JOIN role r ON r.id = rp.role_id
         INNER JOIN permission p ON p.id = rp.permission_id
WHERE r.tenant_id = 0
  AND r.role_code = 'super_admin'
  AND p.permission_code NOT IN ('platform:manage', 'audit:view');
