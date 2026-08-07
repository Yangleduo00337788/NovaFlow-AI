-- Token 用量统计表

CREATE TABLE IF NOT EXISTS `token_usage` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `application_id`  BIGINT UNSIGNED  NULL                    COMMENT '应用ID',
    `agent_id`        BIGINT UNSIGNED  NULL,
    `user_id`         BIGINT UNSIGNED  NULL,
    `model_config_id` BIGINT UNSIGNED  NULL,
    `usage_type`      VARCHAR(16)      NOT NULL DEFAULT 'chat'  COMMENT '类型：chat/embedding/rerank',
    `input_tokens`    INT              NOT NULL DEFAULT 0,
    `output_tokens`   INT              NOT NULL DEFAULT 0,
    `total_tokens`    INT              NOT NULL DEFAULT 0,
    `cost`            DECIMAL(10,6)    NOT NULL DEFAULT 0      COMMENT '费用',
    `latency_ms`      INT              NULL,
    `usage_date`      DATE             NOT NULL                COMMENT '统计日期',
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_date` (`tenant_id`, `usage_date`),
    KEY `idx_agent_date` (`agent_id`, `usage_date`),
    KEY `idx_model_date` (`model_config_id`, `usage_date`),
    KEY `idx_app_date` (`application_id`, `usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token用量统计表';
