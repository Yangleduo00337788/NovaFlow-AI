CREATE TABLE IF NOT EXISTS `department` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `parent_id`       BIGINT UNSIGNED  NULL,
    `dept_name`       VARCHAR(128)     NOT NULL,
    `sort_order`      INT              NOT NULL DEFAULT 0,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_parent` (`tenant_id`, `parent_id`),
    KEY `idx_tenant_name` (`tenant_id`, `dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户部门表';

ALTER TABLE `tenant_member`
    ADD COLUMN `department_id` BIGINT UNSIGNED NULL COMMENT '所属部门' AFTER `role_id`,
    ADD KEY `idx_tenant_department` (`tenant_id`, `department_id`);
