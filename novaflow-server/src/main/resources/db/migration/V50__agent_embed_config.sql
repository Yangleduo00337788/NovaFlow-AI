-- Phase 41: Agent 嵌入配置（主题色 / 域名白名单 / postMessage）

ALTER TABLE `agent`
    ADD COLUMN `embed_config` JSON NULL COMMENT '嵌入配置（主题/域名白名单/postMessage）' AFTER `version`;
