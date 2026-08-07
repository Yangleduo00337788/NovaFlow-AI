-- Model Center 表结构

CREATE TABLE IF NOT EXISTS `model_provider` (
    `id`                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '0为平台级',
    `provider_code`     VARCHAR(32)      NOT NULL                COMMENT '提供商标识：openai/deepseek/qwen/claude/gemini/ollama/custom',
    `provider_name`     VARCHAR(64)      NOT NULL,
    `base_url`          VARCHAR(512)     NULL                    COMMENT 'API Base URL',
    `api_key_encrypted` VARCHAR(512)     NULL                    COMMENT '加密后的API Key',
    `is_enabled`        TINYINT(1)       NOT NULL DEFAULT 1,
    `config`            JSON             NULL                    COMMENT '提供商配置',
    `created_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`        TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_provider` (`tenant_id`, `provider_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型提供商表';

CREATE TABLE IF NOT EXISTS `model_config` (
    `id`                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`           BIGINT UNSIGNED  NOT NULL,
    `provider_id`         BIGINT UNSIGNED  NOT NULL,
    `model_name`          VARCHAR(64)      NOT NULL                COMMENT '模型名称',
    `model_type`          VARCHAR(16)      NOT NULL DEFAULT 'chat' COMMENT '类型：chat/embedding/rerank',
    `display_name`        VARCHAR(64)      NOT NULL                COMMENT '显示名称',
    `context_window`      INT              NOT NULL DEFAULT 4096   COMMENT '上下文窗口',
    `max_output_tokens`   INT              NOT NULL DEFAULT 2048,
    `input_price`         DECIMAL(10,6)    NULL                    COMMENT '输入单价（每1K Token）',
    `output_price`        DECIMAL(10,6)    NULL                    COMMENT '输出单价（每1K Token）',
    `default_temperature` DECIMAL(3,2)     NOT NULL DEFAULT 0.70,
    `is_enabled`          TINYINT(1)       NOT NULL DEFAULT 1,
    `is_default`          TINYINT(1)       NOT NULL DEFAULT 0      COMMENT '是否默认模型',
    `config`              JSON             NULL,
    `created_at`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`          TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_type` (`tenant_id`, `model_type`),
    KEY `idx_provider` (`provider_id`),
    UNIQUE KEY `uk_provider_model` (`provider_id`, `model_name`, `model_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表';
