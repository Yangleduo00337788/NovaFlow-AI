-- RBAC v2 Phase 6：§九 矩阵继续对齐
-- Operator 补充 model:read；Viewer 收回模型/工具/MCP/Prompt/API/账单只读

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code = 'model:read'
WHERE r.tenant_id = 0 AND r.role_code = 'operator';

DELETE rp FROM role_permission rp
INNER JOIN role r ON r.id = rp.role_id AND r.tenant_id = 0 AND r.role_code = 'viewer'
INNER JOIN permission p ON p.id = rp.permission_id
  AND p.permission_code IN (
    'model:read', 'tool:read', 'mcp:read', 'prompt:read', 'api:read', 'billing:view'
  );
