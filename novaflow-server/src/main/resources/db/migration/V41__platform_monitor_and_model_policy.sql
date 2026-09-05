-- Phase 22-23: API 监控阈值与模型供应商白名单默认配置

INSERT INTO `platform_system_config` (`config_key`, `config_value`)
VALUES
    ('api_monitor.hourly_calls_threshold', '500'),
    ('api_monitor.traffic_spike_multiplier', '3'),
    ('model.allowed_provider_codes', '')
ON DUPLICATE KEY UPDATE `config_value` = `config_value`;
