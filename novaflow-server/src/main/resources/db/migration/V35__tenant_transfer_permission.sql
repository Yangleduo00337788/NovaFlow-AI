-- Phase 10：Owner 转移所有权独立权限码；与 tenant:delete 并列仅授予 tenant_owner

INSERT INTO permission (permission_code, permission_name, module) VALUES
('tenant:transfer', '转移企业所有权', 'tenant')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code = 'tenant:transfer'
WHERE r.tenant_id = 0 AND r.role_code = 'tenant_owner';
