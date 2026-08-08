CREATE TABLE IF NOT EXISTS `workflow` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `application_id`  BIGINT UNSIGNED  NOT NULL,
    `workflow_name`   VARCHAR(128)     NOT NULL,
    `description`     VARCHAR(512)     NULL,
    `status`          TINYINT          NOT NULL DEFAULT 0      COMMENT '0-草稿 1-已发布',
    `version`         INT              NOT NULL DEFAULT 1,
    `el_expression`   TEXT             NULL                    COMMENT 'LiteFlow EL表达式',
    `canvas_data`     JSON             NULL                    COMMENT '画布数据（Vue Flow）',
    `created_by`      BIGINT UNSIGNED  NULL,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_app` (`tenant_id`, `application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流表';

CREATE TABLE IF NOT EXISTS `workflow_node` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `workflow_id`     BIGINT UNSIGNED  NOT NULL,
    `node_id`         VARCHAR(64)      NOT NULL                COMMENT '节点唯一标识（画布内）',
    `node_type`       VARCHAR(32)      NOT NULL                COMMENT '节点类型',
    `node_name`       VARCHAR(128)     NOT NULL                COMMENT '节点显示名称',
    `position_x`      DECIMAL(10,2)    NOT NULL DEFAULT 0      COMMENT '画布X坐标',
    `position_y`      DECIMAL(10,2)    NOT NULL DEFAULT 0      COMMENT '画布Y坐标',
    `node_config`     JSON             NOT NULL                COMMENT '节点配置',
    `sort_order`      INT              NOT NULL DEFAULT 0,
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_node` (`workflow_id`, `node_id`),
    KEY `idx_node_type` (`node_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流节点表';

CREATE TABLE IF NOT EXISTS `workflow_edge` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `workflow_id`     BIGINT UNSIGNED  NOT NULL,
    `edge_id`         VARCHAR(64)      NOT NULL                COMMENT '连线唯一标识',
    `source_node_id`  VARCHAR(64)      NOT NULL                COMMENT '源节点ID',
    `target_node_id`  VARCHAR(64)      NOT NULL                COMMENT '目标节点ID',
    `source_handle`   VARCHAR(32)      NULL                    COMMENT '源连接点',
    `target_handle`   VARCHAR(32)      NULL                    COMMENT '目标连接点',
    `condition`       VARCHAR(256)     NULL                    COMMENT '条件表达式',
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_edge` (`workflow_id`, `edge_id`),
    KEY `idx_source` (`workflow_id`, `source_node_id`),
    KEY `idx_target` (`workflow_id`, `target_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流连线表';

CREATE TABLE IF NOT EXISTS `workflow_execution` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `workflow_id`     BIGINT UNSIGNED  NOT NULL,
    `execution_id`    VARCHAR(64)      NOT NULL                COMMENT '执行唯一ID',
    `status`          TINYINT          NOT NULL DEFAULT 0      COMMENT '0-运行中 1-成功 2-失败 3-超时',
    `input_data`      JSON             NULL                    COMMENT '输入数据',
    `output_data`     JSON             NULL                    COMMENT '输出数据',
    `error_message`   TEXT             NULL,
    `started_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `finished_at`     DATETIME         NULL,
    `duration_ms`     INT              NULL                    COMMENT '执行耗时毫秒',
    `triggered_by`    BIGINT UNSIGNED  NULL                    COMMENT '触发人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_execution_id` (`execution_id`),
    KEY `idx_workflow_status` (`workflow_id`, `status`),
    KEY `idx_started_at` (`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流执行记录表';

CREATE TABLE IF NOT EXISTS `workflow_node_log` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED  NOT NULL,
    `execution_id`    VARCHAR(64)      NOT NULL,
    `node_id`         VARCHAR(64)      NOT NULL,
    `node_type`       VARCHAR(32)      NOT NULL,
    `status`          TINYINT          NOT NULL                COMMENT '0-运行中 1-成功 2-失败',
    `input_data`      JSON             NULL,
    `output_data`     JSON             NULL,
    `error_message`   TEXT             NULL,
    `started_at`      DATETIME         NOT NULL,
    `finished_at`     DATETIME         NULL,
    `duration_ms`     INT              NULL,
    PRIMARY KEY (`id`),
    KEY `idx_execution` (`execution_id`),
    KEY `idx_node` (`execution_id`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流节点执行日志';
