CREATE TABLE IF NOT EXISTS `billing_alert` (
    `id`                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT UNSIGNED  NOT NULL,
    `alert_name`        VARCHAR(64)      NOT NULL,
    `alert_type`        VARCHAR(32)      NOT NULL                COMMENT 'token_quota/cost_limit',
    `threshold_percent` INT              NOT NULL                COMMENT '阈值百分比：80/100',
    `notify_channels`   VARCHAR(128)     NOT NULL DEFAULT 'site' COMMENT '通知渠道：site,email',
    `is_enabled`        TINYINT(1)       NOT NULL DEFAULT 1,
    `last_triggered_at` DATETIME         NULL,
    `created_by`        BIGINT UNSIGNED  NULL,
    `created_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_type_threshold` (`tenant_id`, `alert_type`, `threshold_percent`),
    KEY `idx_tenant_type` (`tenant_id`, `alert_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用预警配置表';

CREATE TABLE IF NOT EXISTS `user_notification` (
    `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`   BIGINT UNSIGNED  NOT NULL,
    `user_id`     BIGINT UNSIGNED  NOT NULL,
    `category`    VARCHAR(32)      NOT NULL                COMMENT 'billing/system/...',
    `title`       VARCHAR(128)     NOT NULL,
    `content`     VARCHAR(512)     NOT NULL,
    `link_url`    VARCHAR(256)     NULL,
    `is_read`     TINYINT(1)       NOT NULL DEFAULT 0,
    `created_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`tenant_id`, `user_id`, `is_read`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户站内通知表';
