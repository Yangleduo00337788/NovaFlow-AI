-- RBAC v2：6 租户角色 + 扩展权限码 + 资源级 ACL

CREATE TABLE IF NOT EXISTS `resource_permission` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL                COMMENT '租户 ID',
    `resource_type`   VARCHAR(32)      NOT NULL                COMMENT 'AGENT / WORKFLOW / KNOWLEDGE / APPLICATION',
    `resource_id`     BIGINT UNSIGNED  NOT NULL                COMMENT '资源 ID',
    `user_id`         BIGINT UNSIGNED  NOT NULL                COMMENT '被授权用户 ID',
    `permission_code` VARCHAR(128)     NOT NULL                COMMENT '权限码，如 agent:read、agent:edit',
    `created_by`      BIGINT UNSIGNED  NULL                    COMMENT '授权人',
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_resource_user_perm` (`tenant_id`, `resource_type`, `resource_id`, `user_id`, `permission_code`),
    KEY `idx_tenant_resource` (`tenant_id`, `resource_type`, `resource_id`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB COMMENT='资源级权限（可选收紧；未配置时沿用角色权限）';

-- 扩展权限码（保留既有 agent:edit 等，新增 read / 细粒度码）
INSERT INTO permission (permission_code, permission_name, module) VALUES
('dashboard:view', '查看工作台', 'dashboard'),
('agent:read', '查看 Agent', 'agent'),
('agent:debug', '调试 Agent', 'agent'),
('workflow:read', '查看工作流', 'workflow'),
('workflow:delete', '删除工作流', 'workflow'),
('workflow:publish', '发布工作流', 'workflow'),
('workflow:execute', '执行工作流', 'workflow'),
('knowledge:read', '查看知识库', 'knowledge'),
('knowledge:delete', '删除知识库', 'knowledge'),
('knowledge:search', '检索知识库', 'knowledge'),
('model:read', '查看模型', 'model'),
('tool:read', '查看工具', 'tool'),
('tool:create', '创建工具', 'tool'),
('tool:update', '编辑工具', 'tool'),
('tool:delete', '删除工具', 'tool'),
('mcp:read', '查看 MCP', 'mcp'),
('mcp:create', '创建 MCP', 'mcp'),
('mcp:update', '编辑 MCP', 'mcp'),
('mcp:delete', '删除 MCP', 'mcp'),
('prompt:read', '查看 Prompt', 'prompt'),
('prompt:delete', '删除 Prompt', 'prompt'),
('api:read', '查看 API Key', 'api'),
('api:create', '创建 API Key', 'api'),
('api:update', '编辑 API Key', 'api'),
('api:delete', '删除 API Key', 'api'),
('log:read', '查看调用日志', 'monitor'),
('user:read', '查看用户', 'user'),
('user:create', '创建用户', 'user'),
('user:update', '编辑用户', 'user'),
('user:delete', '删除用户', 'user'),
('role:read', '查看角色', 'role'),
('role:create', '创建角色', 'role'),
('role:update', '编辑角色', 'role'),
('role:delete', '删除角色', 'role'),
('tenant:delete', '删除企业', 'tenant')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- user → member（企业普通成员）
UPDATE role
SET role_code = 'member', role_name = '企业成员'
WHERE tenant_id = 0 AND role_code = 'user';

-- 新增租户角色
INSERT INTO role (tenant_id, role_code, role_name, is_system, is_deleted) VALUES
(0, 'tenant_owner', '企业所有者', 1, 0),
(0, 'operator', '运维人员', 1, 0),
(0, 'viewer', '只读用户', 1, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 企业所有者：企业内全部能力 + 删除企业
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.tenant_id = 0 AND r.role_code = 'tenant_owner'
  AND p.permission_code != 'platform:manage';

-- 企业管理员：除平台总控与删除企业外的全部权限
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.tenant_id = 0 AND r.role_code = 'tenant_admin'
  AND p.permission_code NOT IN ('platform:manage', 'tenant:delete');

-- 超级管理员：全部权限
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.tenant_id = 0 AND r.role_code = 'super_admin';

-- 开发者：按矩阵授予（不含模型配置、组织与账单管理）
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code IN (
    'dashboard:view',
    'agent:read', 'agent:create', 'agent:edit', 'agent:delete', 'agent:publish', 'agent:chat', 'agent:debug',
    'workflow:read', 'workflow:create', 'workflow:edit', 'workflow:delete', 'workflow:publish', 'workflow:execute',
    'knowledge:read', 'knowledge:create', 'knowledge:upload', 'knowledge:delete', 'knowledge:search',
    'model:read',
    'tool:read', 'tool:create', 'tool:update', 'tool:delete',
    'mcp:read', 'mcp:create', 'mcp:update', 'mcp:delete',
    'prompt:read', 'prompt:create', 'prompt:edit', 'prompt:delete',
    'api:read', 'api:create', 'api:update', 'api:delete',
    'application:manage',
    'monitor:view', 'trace:view', 'log:read',
    'billing:view',
    'search:global',
    'portal:access'
)
WHERE r.tenant_id = 0 AND r.role_code = 'developer';

DELETE rp FROM role_permission rp
INNER JOIN role r ON r.id = rp.role_id AND r.tenant_id = 0 AND r.role_code = 'developer'
INNER JOIN permission p ON p.id = rp.permission_id AND p.permission_code = 'model:config';

-- 运维：运行发布与观测，不改核心配置
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code IN (
    'dashboard:view',
    'agent:read', 'agent:publish', 'agent:chat',
    'workflow:read', 'workflow:publish', 'workflow:execute',
    'knowledge:read', 'knowledge:search',
    'application:manage',
    'monitor:view', 'trace:view', 'log:read',
    'billing:view',
    'search:global',
    'portal:access'
)
WHERE r.tenant_id = 0 AND r.role_code = 'operator';

-- 企业成员：使用 AI 应用（可进工作台与门户）
DELETE rp FROM role_permission rp
INNER JOIN role r ON r.id = rp.role_id AND r.tenant_id = 0 AND r.role_code = 'member';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code IN (
    'dashboard:view',
    'agent:read', 'agent:chat',
    'workflow:read',
    'knowledge:read', 'knowledge:search',
    'portal:access',
    'search:global'
)
WHERE r.tenant_id = 0 AND r.role_code = 'member';

-- 只读：全模块查看，无写权限
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code IN (
    'dashboard:view',
    'agent:read',
    'workflow:read',
    'knowledge:read', 'knowledge:search',
    'model:read',
    'tool:read', 'mcp:read',
    'prompt:read',
    'api:read',
    'application:manage',
    'monitor:view', 'trace:view', 'log:read',
    'billing:view',
    'search:global',
    'portal:access'
)
WHERE r.tenant_id = 0 AND r.role_code = 'viewer';

-- 为既有角色补充 read 类权限（兼容旧注解 + 新菜单）
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code IN (
    'dashboard:view', 'agent:read', 'workflow:read', 'knowledge:read', 'knowledge:search',
    'model:read', 'tool:read', 'mcp:read', 'prompt:read', 'api:read', 'log:read',
    'workflow:delete', 'workflow:publish', 'workflow:execute', 'knowledge:delete',
    'agent:debug', 'prompt:delete',
    'user:read', 'user:create', 'user:update', 'user:delete',
    'role:read', 'role:create', 'role:update', 'role:delete'
)
WHERE r.tenant_id = 0 AND r.role_code IN ('tenant_admin', 'super_admin');

-- 演示企业管理员提升为 Owner（幂等：仅当仍为 tenant_admin 时）
UPDATE tenant_member tm
INNER JOIN role old_role ON old_role.id = tm.role_id AND old_role.role_code = 'tenant_admin'
INNER JOIN role owner_role ON owner_role.tenant_id = 0 AND owner_role.role_code = 'tenant_owner'
INNER JOIN tenant t ON t.id = tm.tenant_id AND t.tenant_code = 'demo'
SET tm.role_id = owner_role.id
WHERE tm.is_deleted = 0;
