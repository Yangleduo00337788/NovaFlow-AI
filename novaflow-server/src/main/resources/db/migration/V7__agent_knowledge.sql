CREATE TABLE IF NOT EXISTS `agent_knowledge` (
    `id`                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT UNSIGNED  NOT NULL,
    `agent_id`          BIGINT UNSIGNED  NOT NULL,
    `knowledge_base_id` BIGINT UNSIGNED  NOT NULL,
    `created_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_kb` (`agent_id`, `knowledge_base_id`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_knowledge_base_id` (`knowledge_base_id`)
) ENGINE=InnoDB COMMENT='Agent知识库关联表';
