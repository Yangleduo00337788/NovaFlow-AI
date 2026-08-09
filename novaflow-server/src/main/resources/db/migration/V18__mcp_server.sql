-- MCP 服务注册表

CREATE TABLE IF NOT EXISTS `mcp_server` (
    `id`                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT UNSIGNED  NOT NULL,
    `server_name`       VARCHAR(64)      NOT NULL,
    `description`       VARCHAR(512)     NULL,
    `transport_type`    VARCHAR(16)      NOT NULL                COMMENT '传输类型：stdio/sse/http',
    `server_config`     JSON             NOT NULL                COMMENT '服务配置',
    `discovered_tools`  JSON             NULL                    COMMENT '自动发现的工具列表',
    `status`            TINYINT          NOT NULL DEFAULT 0      COMMENT '0-未连接 1-已连接 2-错误',
    `last_connected_at` DATETIME         NULL,
    `created_by`        BIGINT UNSIGNED  NULL,
    `created_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`        TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP服务表';
