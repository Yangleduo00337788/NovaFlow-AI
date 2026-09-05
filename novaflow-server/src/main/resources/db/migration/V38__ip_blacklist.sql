-- Phase 15：平台 IP 黑名单

CREATE TABLE IF NOT EXISTS `ip_blacklist` (
    `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `ip_address`  VARCHAR(64)      NOT NULL                COMMENT 'IP 地址',
    `reason`      VARCHAR(512)     NULL                    COMMENT '封禁原因',
    `status`      TINYINT          NOT NULL DEFAULT 1      COMMENT '1=生效 0=停用',
    `expire_at`   DATETIME         NULL                    COMMENT '过期时间，空=永久',
    `created_by`  BIGINT UNSIGNED  NULL,
    `created_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`  TINYINT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_ip_blacklist_ip` (`ip_address`, `is_deleted`),
    KEY `idx_ip_blacklist_status` (`status`, `is_deleted`)
) ENGINE=InnoDB COMMENT='平台 IP 黑名单';
