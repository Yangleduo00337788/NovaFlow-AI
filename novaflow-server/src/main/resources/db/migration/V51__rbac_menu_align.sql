-- 调用日志仅 log:read；企业成员仅门户（portal:access + agent:chat）

DELETE rp FROM role_permission rp
INNER JOIN role r ON r.id = rp.role_id AND r.tenant_id = 0 AND r.role_code = 'member'
INNER JOIN permission p ON p.id = rp.permission_id
  AND p.permission_code NOT IN ('portal:access', 'agent:chat');
