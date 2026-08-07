-- NovaFlow AI 核心表结构（MVP）

CREATE TABLE IF NOT EXISTS `user` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `username`        VARCHAR(64)      NOT NULL,
    `email`           VARCHAR(128)     NOT NULL,
    `password_hash`   VARCHAR(256)     NOT NULL,
    `nickname`        VARCHAR(64)      NULL,
    `avatar_url`      VARCHAR(512)     NULL,
    `phone`           VARCHAR(20)      NULL,
    `status`          TINYINT          NOT NULL DEFAULT 1,
    `last_login_at`   DATETIME         NULL,
    `last_login_ip`   VARCHAR(45)      NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `role` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL DEFAULT 0,
    `role_code`       VARCHAR(64)      NOT NULL,
    `role_name`       VARCHAR(64)      NOT NULL,
    `description`     VARCHAR(256)     NULL,
    `is_system`       TINYINT(1)       NOT NULL DEFAULT 0,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `permission` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `permission_code` VARCHAR(128)     NOT NULL,
    `permission_name` VARCHAR(64)      NOT NULL,
    `module`          VARCHAR(32)      NOT NULL,
    `description`     VARCHAR(256)     NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE IF NOT EXISTS `role_permission` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `role_id`         BIGINT UNSIGNED  NOT NULL,
    `permission_id`   BIGINT UNSIGNED  NOT NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS `tenant` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_code`     VARCHAR(64)      NOT NULL,
    `tenant_name`     VARCHAR(128)     NOT NULL,
    `logo_url`        VARCHAR(512)     NULL,
    `contact_name`    VARCHAR(64)      NULL,
    `contact_email`   VARCHAR(128)     NULL,
    `contact_phone`   VARCHAR(20)      NULL,
    `plan_type`       VARCHAR(32)      NOT NULL DEFAULT 'enterprise',
    `status`          TINYINT          NOT NULL DEFAULT 1,
    `expire_at`       DATETIME         NULL,
    `max_members`     INT              NOT NULL DEFAULT 100,
    `max_agents`      INT              NOT NULL DEFAULT 500,
    `max_knowledge`   INT              NOT NULL DEFAULT 100,
    `max_storage_mb`  INT            NOT NULL DEFAULT 10240,
    `monthly_token_quota` BIGINT       NOT NULL DEFAULT 100000000,
    `config`          JSON             NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

CREATE TABLE IF NOT EXISTS `tenant_member` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `user_id`         BIGINT UNSIGNED  NOT NULL,
    `role_id`         BIGINT UNSIGNED  NOT NULL,
    `status`          TINYINT          NOT NULL DEFAULT 1,
    `joined_at`       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户成员表';

CREATE TABLE IF NOT EXISTS `workspace` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `workspace_name`  VARCHAR(128)     NOT NULL,
    `description`     VARCHAR(512)     NULL,
    `icon`            VARCHAR(64)      NULL,
    `is_default`      TINYINT(1)       NOT NULL DEFAULT 0,
    `created_by`      BIGINT UNSIGNED  NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作空间表';

CREATE TABLE IF NOT EXISTS `application` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `workspace_id`    BIGINT UNSIGNED  NOT NULL,
    `app_name`        VARCHAR(128)     NOT NULL,
    `description`     VARCHAR(512)     NULL,
    `icon`            VARCHAR(64)      NULL,
    `app_type`        VARCHAR(32)      NOT NULL DEFAULT 'agent',
    `default_agent_id` BIGINT UNSIGNED NULL,
    `publish_status`  TINYINT          NOT NULL DEFAULT 0,
    `access_type`     VARCHAR(16)      NOT NULL DEFAULT 'team',
    `invoke_count`    BIGINT           NOT NULL DEFAULT 0,
    `published_at`    DATETIME         NULL,
    `status`          TINYINT          NOT NULL DEFAULT 1,
    `created_by`      BIGINT UNSIGNED  NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_workspace` (`tenant_id`, `workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用表';

CREATE TABLE IF NOT EXISTS `billing_quota` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `plan_type`       VARCHAR(32)      NOT NULL,
    `max_members`     INT              NOT NULL DEFAULT 100,
    `used_members`    INT              NOT NULL DEFAULT 0,
    `max_agents`      INT              NOT NULL DEFAULT 500,
    `max_knowledge`   INT              NOT NULL DEFAULT 100,
    `max_storage_mb`  INT              NOT NULL DEFAULT 10240,
    `monthly_token_quota` BIGINT       NOT NULL DEFAULT 100000000,
    `used_tokens`     BIGINT           NOT NULL DEFAULT 0,
    `monthly_cost_limit` DECIMAL(12,2) NULL,
    `used_cost`       DECIMAL(12,4)    NOT NULL DEFAULT 0,
    `quota_reset_at`  DATE             NOT NULL,
    `expire_at`       DATETIME         NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户配额表';

CREATE TABLE IF NOT EXISTS `user_recent_access` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `user_id`         BIGINT UNSIGNED  NOT NULL,
    `resource_type`   VARCHAR(16)      NOT NULL,
    `resource_id`     BIGINT UNSIGNED  NOT NULL,
    `resource_name`   VARCHAR(256)     NOT NULL,
    `accessed_at`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_resource` (`user_id`, `resource_type`, `resource_id`),
    KEY `idx_tenant_user_time` (`tenant_id`, `user_id`, `accessed_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户最近访问记录表';

CREATE TABLE IF NOT EXISTS `agent` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `application_id`  BIGINT UNSIGNED  NOT NULL,
    `agent_name`      VARCHAR(128)     NOT NULL,
    `description`     VARCHAR(512)     NULL,
    `icon`            VARCHAR(64)      NULL,
    `agent_type`      VARCHAR(32)      NOT NULL DEFAULT 'chat',
    `status`          TINYINT          NOT NULL DEFAULT 0,
    `published_at`    DATETIME         NULL,
    `version`         INT              NOT NULL DEFAULT 1,
    `created_by`      BIGINT UNSIGNED  NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_app` (`tenant_id`, `application_id`),
    KEY `idx_agent_type` (`agent_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent表';

CREATE TABLE IF NOT EXISTS `agent_config` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `agent_id`        BIGINT UNSIGNED  NOT NULL,
    `system_prompt`   MEDIUMTEXT       NULL,
    `prompt_template_id` BIGINT UNSIGNED NULL,
    `prompt_template_version_id` BIGINT UNSIGNED NULL,
    `prompt_ref_mode` VARCHAR(16)      NULL,
    `welcome_message` VARCHAR(512)     NULL,
    `suggested_questions` JSON         NULL,
    `model_config_id` BIGINT UNSIGNED  NULL,
    `temperature`     DECIMAL(3,2)     NOT NULL DEFAULT 0.70,
    `max_tokens`      INT              NOT NULL DEFAULT 2048,
    `top_p`           DECIMAL(3,2)     NULL,
    `memory_type`     VARCHAR(16)      NOT NULL DEFAULT 'window',
    `memory_window`   INT              NOT NULL DEFAULT 10,
    `workflow_id`     BIGINT UNSIGNED  NULL,
    `retrieval_config` JSON            NULL,
    `extra_config`    JSON             NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent配置表';
