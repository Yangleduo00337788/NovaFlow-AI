-- 链路分析与调用日志增强

ALTER TABLE `token_usage`
    ADD COLUMN `error_message` VARCHAR(512) NULL COMMENT '失败原因' AFTER `success`,
    ADD COLUMN `trace_id` VARCHAR(64) NULL COMMENT '关联 trace/execution id' AFTER `error_message`;
