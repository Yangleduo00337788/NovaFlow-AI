-- 操作审计日志

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `user_id`         BIGINT UNSIGNED  NULL,
    `action`          VARCHAR(64)      NOT NULL                COMMENT '操作类型',
    `resource_type`   VARCHAR(64)      NOT NULL                COMMENT '资源类型',
    `resource_id`     BIGINT UNSIGNED  NULL,
    `detail`          VARCHAR(512)     NULL                    COMMENT '操作摘要（不含敏感内容）',
    `client_ip`       VARCHAR(64)      NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_created` (`tenant_id`, `created_at` DESC),
    KEY `idx_tenant_action` (`tenant_id`, `action`)
) ENGINE=InnoDB COMMENT='操作审计日志';
