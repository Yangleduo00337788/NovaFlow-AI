-- 对话会话持久化

CREATE TABLE IF NOT EXISTS `conversation` (
    `id`                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT UNSIGNED  NOT NULL,
    `agent_id`          BIGINT UNSIGNED  NOT NULL,
    `conversation_key`  VARCHAR(128)     NOT NULL                COMMENT '客户端会话标识',
    `channel`           VARCHAR(16)      NOT NULL DEFAULT 'debug' COMMENT '渠道：debug/open',
    `user_id`           BIGINT UNSIGNED  NULL,
    `message_count`     INT              NOT NULL DEFAULT 0,
    `last_message_at`   DATETIME         NULL,
    `created_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_conversation` (`agent_id`, `conversation_key`),
    KEY `idx_tenant_agent` (`tenant_id`, `agent_id`),
    KEY `idx_tenant_last_message` (`tenant_id`, `last_message_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 对话会话表';

CREATE TABLE IF NOT EXISTS `conversation_message` (
    `id`                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT UNSIGNED  NOT NULL,
    `conversation_id`   BIGINT UNSIGNED  NOT NULL,
    `role`              VARCHAR(16)      NOT NULL                COMMENT 'user/assistant',
    `content`           MEDIUMTEXT       NOT NULL,
    `tokens_used`       INT              NULL,
    `latency_ms`        BIGINT           NULL,
    `retrieval_sources` JSON             NULL,
    `created_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conversation_time` (`conversation_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息表';
