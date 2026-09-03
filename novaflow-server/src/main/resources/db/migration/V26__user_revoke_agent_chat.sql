-- 普通用户不得持有 agent:chat：门户对话走 portal:access + 已发布应用校验。
-- 此前同时授予 agent:chat 会使 assertPortalAccess 被跳过，从而列出/调试未发布 Agent。
DELETE rp FROM role_permission rp
INNER JOIN role r ON r.id = rp.role_id AND r.tenant_id = 0 AND r.role_code = 'user' AND r.is_deleted = 0
INNER JOIN permission p ON p.id = rp.permission_id AND p.permission_code = 'agent:chat';
