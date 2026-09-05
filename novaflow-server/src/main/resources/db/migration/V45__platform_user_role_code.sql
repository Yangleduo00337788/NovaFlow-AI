-- Phase 30：平台账号子角色（超管 / 审计员）

ALTER TABLE `user`
    ADD COLUMN `platform_role_code` VARCHAR(64) NULL COMMENT '平台账号子角色：super_admin / platform_auditor' AFTER `account_type`;

UPDATE `user`
SET `platform_role_code` = 'super_admin'
WHERE `account_type` = 'platform'
  AND (`platform_role_code` IS NULL OR `platform_role_code` = '');
