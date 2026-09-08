-- 收敛为三种系统角色：平台管理员 / 企业管理员 / 普通用户

UPDATE role SET role_name = '平台管理员' WHERE tenant_id = 0 AND role_code = 'super_admin';
UPDATE role SET role_name = '企业管理员' WHERE tenant_id = 0 AND role_code = 'tenant_admin';
UPDATE role SET role_name = '普通用户' WHERE tenant_id = 0 AND role_code = 'member';

-- 原 Owner / 开发者 / 运维 → 企业管理员（保留 Studio 能力）
UPDATE tenant_member tm
INNER JOIN role old_role ON old_role.id = tm.role_id
INNER JOIN role admin_role ON admin_role.tenant_id = 0 AND admin_role.role_code = 'tenant_admin' AND admin_role.is_deleted = 0
SET tm.role_id = admin_role.id
WHERE tm.is_deleted = 0
  AND old_role.role_code IN ('tenant_owner', 'developer', 'operator');

-- 原只读 Viewer → 普通用户（门户）
UPDATE tenant_member tm
INNER JOIN role old_role ON old_role.id = tm.role_id
INNER JOIN role user_role ON user_role.tenant_id = 0 AND user_role.role_code = 'member' AND user_role.is_deleted = 0
SET tm.role_id = user_role.id
WHERE tm.is_deleted = 0
  AND old_role.role_code = 'viewer';

-- 企业管理员：本企业全部权限（不含平台总控码）
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.tenant_id = 0
  AND r.role_code = 'tenant_admin'
  AND r.is_deleted = 0
  AND p.permission_code NOT LIKE 'platform:%'
  AND p.permission_code != 'tenant:transfer';

-- 平台账号一律平台管理员
UPDATE `user`
SET platform_role_code = 'super_admin'
WHERE is_deleted = 0
  AND account_type = 'platform';

-- 平台管理员：全部 platform:* 与 audit:view
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.tenant_id = 0
  AND r.role_code = 'super_admin'
  AND r.is_deleted = 0
  AND (p.permission_code LIKE 'platform:%' OR p.permission_code = 'audit:view');

-- 下线不再使用的系统角色（保留行以免历史外键，标记删除）
UPDATE role
SET is_deleted = 1
WHERE tenant_id = 0
  AND role_code IN (
    'tenant_owner',
    'developer',
    'operator',
    'viewer',
    'platform_auditor',
    'platform_support',
    'platform_billing'
  );
