-- 统计与链路查询性能索引

ALTER TABLE `token_usage`
    ADD KEY `idx_tenant_created` (`tenant_id`, `created_at`),
    ADD KEY `idx_tenant_trace` (`tenant_id`, `trace_id`),
    ADD KEY `idx_tenant_success_created` (`tenant_id`, `success`, `created_at`);

ALTER TABLE `workflow_execution`
    ADD KEY `idx_tenant_started` (`tenant_id`, `started_at`);

ALTER TABLE `workflow`
    ADD KEY `idx_tenant_status_created` (`tenant_id`, `is_deleted`, `status`, `created_at`);

ALTER TABLE `conversation_message`
    ADD KEY `idx_conv_role_time` (`conversation_id`, `role`, `created_at`);
