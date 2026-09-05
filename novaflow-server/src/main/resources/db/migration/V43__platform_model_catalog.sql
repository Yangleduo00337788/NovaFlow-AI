-- Phase 29：平台模型目录与定价

CREATE TABLE IF NOT EXISTS `platform_model_catalog` (
    `id`                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `provider_code`       VARCHAR(64)      NOT NULL,
    `model_name`          VARCHAR(128)     NOT NULL,
    `display_name`        VARCHAR(128)     NULL,
    `model_type`          VARCHAR(32)      NOT NULL DEFAULT 'chat' COMMENT 'chat / embedding / rerank',
    `input_price_per_1k`  DECIMAL(12, 6)   NULL,
    `output_price_per_1k` DECIMAL(12, 6)   NULL,
    `currency`            VARCHAR(8)       NOT NULL DEFAULT 'CNY',
    `enabled`             TINYINT          NOT NULL DEFAULT 1,
    `description`         VARCHAR(512)     NULL,
    `created_at`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`          TINYINT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_model` (`provider_code`, `model_name`, `is_deleted`)
) ENGINE=InnoDB COMMENT='平台模型目录（统一定价与开放范围）';

INSERT INTO platform_model_catalog (provider_code, model_name, display_name, model_type, input_price_per_1k, output_price_per_1k, currency, enabled, description)
VALUES
('deepseek', 'deepseek-chat', 'DeepSeek Chat', 'chat', 0.001000, 0.002000, 'CNY', 1, '通用对话'),
('qwen', 'qwen-plus', '通义千问 Plus', 'chat', 0.000800, 0.002000, 'CNY', 1, '通义对话'),
('openai', 'gpt-4o-mini', 'GPT-4o Mini', 'chat', 0.000150, 0.000600, 'USD', 1, 'OpenAI 轻量对话')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);
