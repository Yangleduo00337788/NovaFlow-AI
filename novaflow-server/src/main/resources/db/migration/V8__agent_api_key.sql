CREATE TABLE IF NOT EXISTS `agent_api_key` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `agent_id`        BIGINT UNSIGNED  NOT NULL,
    `api_key_hash`    VARCHAR(64)      NOT NULL                COMMENT 'API Key SHA-256',
    `api_key_prefix`  VARCHAR(16)      NOT NULL                COMMENT 'Key前缀（用于展示）',
    `status`          TINYINT          NOT NULL DEFAULT 1      COMMENT '1-启用 0-禁用',
    `last_used_at`    DATETIME         NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_id` (`agent_id`),
    UNIQUE KEY `uk_api_key_hash` (`api_key_hash`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB COMMENT='Agent API Key表';
