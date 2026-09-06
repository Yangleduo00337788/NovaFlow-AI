-- Phase 32：平台安全风控告警
-- Phase 33：存储配额（配置项，用量由 document 表聚合）

CREATE TABLE IF NOT EXISTS `platform_security_alert_event` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `alert_type`   VARCHAR(32)     NOT NULL                COMMENT 'ABNORMAL_LOGIN / BATCH_REGISTER / NEW_USER_AGENT',
    `severity`     VARCHAR(16)     NOT NULL                COMMENT 'warning / critical',
    `user_id`      BIGINT UNSIGNED NULL,
    `user_email`   VARCHAR(128)    NULL,
    `tenant_id`    BIGINT UNSIGNED NULL,
    `client_ip`    VARCHAR(64)     NULL,
    `user_agent`   VARCHAR(512)    NULL,
    `message`      VARCHAR(512)    NOT NULL,
    `metric_value` BIGINT          NULL,
    `threshold`    BIGINT          NULL,
    `status`       VARCHAR(16)     NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / ACKED',
    `acked_by`     BIGINT UNSIGNED NULL,
    `acked_at`     DATETIME        NULL,
    `event_date`   DATE            NOT NULL,
    `dedupe_key`   VARCHAR(128)    NOT NULL,
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_security_alert_dedupe` (`alert_type`, `dedupe_key`, `event_date`),
    KEY `idx_security_alert_status` (`status`, `created_at`)
) ENGINE=InnoDB COMMENT='平台安全风控告警事件';

INSERT INTO `platform_system_config` (`config_key`, `config_value`, `updated_at`)
VALUES
    ('risk.abnormal_login_enabled', 'true', NOW()),
    ('risk.batch_register_ip_limit_per_day', '5', NOW()),
    ('risk.new_user_agent_enabled', 'true', NOW()),
    ('risk.storage_warn_percent', '80', NOW())
ON DUPLICATE KEY UPDATE `config_key` = `config_key`;
