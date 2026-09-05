-- Phase 11: 平台账号与租户账号拆分（account_type）+ 自定义角色数据模型就绪

ALTER TABLE `user`
    ADD COLUMN `account_type` VARCHAR(16) NOT NULL DEFAULT 'tenant'
        COMMENT 'tenant=企业账号 platform=平台账号'
        AFTER `status`;

UPDATE `user` SET `account_type` = 'platform' WHERE `email` = 'platform@novaflow.ai' AND `is_deleted` = 0;

UPDATE `tenant_member` tm
    INNER JOIN `user` u ON u.id = tm.user_id AND u.is_deleted = 0
SET tm.is_deleted = 1, tm.updated_at = NOW()
WHERE u.account_type = 'platform' AND tm.is_deleted = 0;
