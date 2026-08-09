-- 工作台增强：收藏表 + 调用成功状态

CREATE TABLE IF NOT EXISTS `user_favorite` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `user_id`         BIGINT UNSIGNED  NOT NULL,
    `resource_type`   VARCHAR(16)      NOT NULL                COMMENT 'agent/workflow/knowledge',
    `resource_id`     BIGINT UNSIGNED  NOT NULL,
    `resource_name`   VARCHAR(256)     NOT NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_favorite` (`user_id`, `resource_type`, `resource_id`),
    KEY `idx_tenant_user_time` (`tenant_id`, `user_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏';

ALTER TABLE `token_usage`
    ADD COLUMN `success` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1-成功 0-失败' AFTER `latency_ms`;
