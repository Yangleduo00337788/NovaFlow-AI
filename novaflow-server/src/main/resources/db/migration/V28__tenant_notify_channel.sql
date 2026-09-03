CREATE TABLE IF NOT EXISTS `tenant_notify_channel` (
    `id`                 BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT UNSIGNED  NOT NULL,
    `email_enabled`      TINYINT(1)       NOT NULL DEFAULT 0,
    `email_recipients`   VARCHAR(512)     NULL COMMENT '逗号分隔邮箱，空则回落到企业联系人/管理员邮箱',
    `webhook_enabled`    TINYINT(1)       NOT NULL DEFAULT 0,
    `webhook_url`        VARCHAR(512)     NULL,
    `webhook_secret`     VARCHAR(128)    NULL,
    `created_at`         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户外部告警通道';
