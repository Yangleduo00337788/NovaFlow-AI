-- Phase 20: 平台系统配置（运行时可调）

CREATE TABLE IF NOT EXISTS `platform_system_config` (
    `config_key`   VARCHAR(64)  NOT NULL PRIMARY KEY COMMENT '配置键',
    `config_value` VARCHAR(512) NOT NULL COMMENT '配置值',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by`   BIGINT UNSIGNED NULL COMMENT '最后修改人'
) ENGINE=InnoDB COMMENT='平台系统配置';

INSERT INTO `platform_system_config` (`config_key`, `config_value`)
VALUES ('auth.registration_enabled', 'true')
ON DUPLICATE KEY UPDATE `config_value` = `config_value`;
