CREATE TABLE IF NOT EXISTS `agent_skill` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `agent_id`        BIGINT UNSIGNED  NOT NULL,
    `skill_id`        BIGINT UNSIGNED  NOT NULL COMMENT 'tool_definition.id，tool_type=skill',
    `sort_order`      INT              NOT NULL DEFAULT 0,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_skill` (`agent_id`, `skill_id`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent技能关联表';
