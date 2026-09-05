-- Phase 30：平台审计员子角色 + 配置中心扩展

INSERT INTO role (tenant_id, role_code, role_name, is_system, is_deleted) VALUES
(0, 'platform_auditor', '平台审计员', 1, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code IN ('audit:view')
WHERE r.tenant_id = 0 AND r.role_code = 'platform_auditor';

INSERT INTO platform_system_config (config_key, config_value, updated_at) VALUES
('platform.maintenance_enabled', 'false', NOW()),
('platform.maintenance_message', '', NOW()),
('platform.announcement', '', NOW())
ON DUPLICATE KEY UPDATE config_key = config_key;
