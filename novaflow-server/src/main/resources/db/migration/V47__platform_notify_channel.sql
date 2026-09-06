-- Phase 35：平台级外部告警通道（邮件 / Webhook）

CREATE TABLE IF NOT EXISTS `platform_notify_channel` (
    `id`                 BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `email_enabled`      TINYINT(1)       NOT NULL DEFAULT 0,
    `email_recipients`   VARCHAR(512)     NULL COMMENT '逗号分隔邮箱，空则回落到平台账号邮箱',
    `webhook_enabled`    TINYINT(1)       NOT NULL DEFAULT 0,
    `webhook_url`        VARCHAR(512)     NULL,
    `webhook_secret`     VARCHAR(128)     NULL,
    `created_at`         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台外部告警通道（单行）';

INSERT INTO `platform_notify_channel` (`id`, `email_enabled`, `webhook_enabled`, `created_at`, `updated_at`)
VALUES (1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO `platform_system_config` (`config_key`, `config_value`, `updated_at`)
VALUES
    ('notify.security_channels', '', NOW()),
    ('notify.api_monitor_channels', '', NOW()),
    ('notify.storage_quota_channels', '', NOW())
ON DUPLICATE KEY UPDATE `config_key` = `config_key`;
