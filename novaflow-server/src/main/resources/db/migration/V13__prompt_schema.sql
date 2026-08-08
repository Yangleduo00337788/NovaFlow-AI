CREATE TABLE IF NOT EXISTS `prompt_template` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `template_name`   VARCHAR(128)     NOT NULL                COMMENT '模板名称',
    `description`     VARCHAR(512)     NULL,
    `category`        VARCHAR(32)      NOT NULL DEFAULT 'custom' COMMENT '分类',
    `content`         MEDIUMTEXT       NOT NULL                COMMENT '当前版本Prompt正文',
    `variables`       JSON             NULL                    COMMENT '变量定义列表',
    `visibility`      VARCHAR(16)      NOT NULL DEFAULT 'private' COMMENT '可见性',
    `current_version` INT              NOT NULL DEFAULT 1      COMMENT '当前版本号',
    `usage_count`     INT              NOT NULL DEFAULT 0      COMMENT '被引用次数',
    `status`          TINYINT          NOT NULL DEFAULT 1,
    `created_by`      BIGINT UNSIGNED  NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_category` (`tenant_id`, `category`),
    KEY `idx_tenant_name` (`tenant_id`, `template_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt模板表';

CREATE TABLE IF NOT EXISTS `prompt_template_version` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `template_id`     BIGINT UNSIGNED  NOT NULL,
    `version`         INT              NOT NULL                COMMENT '版本号',
    `content`         MEDIUMTEXT       NOT NULL                COMMENT '该版本Prompt内容',
    `variables`       JSON             NULL,
    `change_log`      VARCHAR(512)     NULL                    COMMENT '变更说明',
    `published_by`    BIGINT UNSIGNED  NULL,
    `published_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_version` (`template_id`, `version`),
    KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt模板版本表';
