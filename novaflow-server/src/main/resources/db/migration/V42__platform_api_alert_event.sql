-- Phase 28：平台 API 监控告警事件（历史与处置）

CREATE TABLE IF NOT EXISTS `platform_api_alert_event` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `alert_type`   VARCHAR(32)     NOT NULL                COMMENT 'HIGH_FREQUENCY / TRAFFIC_SPIKE',
    `severity`     VARCHAR(16)     NOT NULL                COMMENT 'warning / critical',
    `tenant_id`    BIGINT UNSIGNED NULL,
    `tenant_name`  VARCHAR(128)    NULL,
    `message`      VARCHAR(512)    NOT NULL,
    `metric_value` BIGINT          NULL,
    `threshold`    BIGINT          NULL,
    `status`       VARCHAR(16)     NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / ACKED',
    `acked_by`     BIGINT UNSIGNED NULL,
    `acked_at`     DATETIME        NULL,
    `event_date`   DATE            NOT NULL                COMMENT '告警归属日期（去重）',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_alert_dedupe` (`alert_type`, `tenant_id`, `event_date`),
    KEY `idx_alert_status` (`status`, `created_at`)
) ENGINE=InnoDB COMMENT='平台 API 监控告警事件';
