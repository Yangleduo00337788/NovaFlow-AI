CREATE TABLE IF NOT EXISTS `tool_definition` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `tool_name`       VARCHAR(64)      NOT NULL                COMMENT '工具标识，租户内唯一',
    `display_name`    VARCHAR(128)     NOT NULL                COMMENT '显示名称',
    `description`     VARCHAR(512)     NOT NULL DEFAULT ''     COMMENT '工具描述',
    `tool_type`       VARCHAR(16)      NOT NULL DEFAULT 'http' COMMENT '工具类型',
    `tool_config`     JSON             NOT NULL                COMMENT 'HTTP 工具配置',
    `is_enabled`      TINYINT(1)       NOT NULL DEFAULT 1,
    `is_public`       TINYINT(1)       NOT NULL DEFAULT 0,
    `created_by`      BIGINT UNSIGNED  NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_tool_name` (`tenant_id`, `tool_name`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具定义表';

CREATE TABLE IF NOT EXISTS `agent_tool` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `agent_id`        BIGINT UNSIGNED  NOT NULL,
    `tool_id`         BIGINT UNSIGNED  NOT NULL,
    `sort_order`      INT              NOT NULL DEFAULT 0,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_tool` (`agent_id`, `tool_id`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_tool_id` (`tool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent工具关联表';
