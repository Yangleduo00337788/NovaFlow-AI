-- Phase 37：平台子角色扩展（客服 / 计费）

INSERT INTO permission (permission_code, permission_name, module) VALUES
('platform:tenant:view', '平台租户查看', 'platform'),
('platform:tenant:manage', '平台租户运营', 'platform'),
('platform:billing:view', '平台计费查看', 'platform')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT INTO role (tenant_id, role_code, role_name, is_system, is_deleted) VALUES
(0, 'platform_support', '平台客服', 1, 0),
(0, 'platform_billing', '平台计费', 1, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code IN (
    'platform:tenant:view',
    'platform:tenant:manage',
    'search:global'
)
WHERE r.tenant_id = 0 AND r.role_code = 'platform_support';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code IN (
    'platform:billing:view',
    'platform:tenant:view',
    'search:global'
)
WHERE r.tenant_id = 0 AND r.role_code = 'platform_billing';
