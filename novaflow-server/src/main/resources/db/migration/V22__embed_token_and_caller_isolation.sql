-- 嵌入页受限 Token + Open API 终端用户会话隔离

CREATE TABLE IF NOT EXISTS `agent_embed_token` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `agent_id`        BIGINT UNSIGNED  NOT NULL,
    `token_hash`      VARCHAR(64)      NOT NULL                COMMENT 'Embed Token SHA-256',
    `token_prefix`    VARCHAR(20)      NOT NULL                COMMENT 'Token 前缀（用于展示）',
    `status`          TINYINT          NOT NULL DEFAULT 1      COMMENT '1-启用 0-禁用',
    `last_used_at`    DATETIME         NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_embed` (`agent_id`),
    UNIQUE KEY `uk_embed_token_hash` (`token_hash`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB COMMENT='Agent 嵌入页受限 Token（仅对话，不可列举会话）';

ALTER TABLE `conversation`
    ADD COLUMN `caller_id` VARCHAR(128) NULL COMMENT 'Open 渠道终端用户标识' AFTER `user_id`,
    ADD KEY `idx_agent_channel_caller` (`agent_id`, `channel`, `caller_id`);
