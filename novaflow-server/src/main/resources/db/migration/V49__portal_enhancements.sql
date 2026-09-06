-- Phase 39/40: 门户分类、收藏与品牌化

ALTER TABLE `tenant`
    ADD COLUMN `portal_theme_color` VARCHAR(32) NULL COMMENT '门户主题色 HEX，如 #6366f1' AFTER `logo_url`;

ALTER TABLE `application`
    ADD COLUMN `portal_category` VARCHAR(64) NOT NULL DEFAULT 'general' COMMENT '门户展示分类' AFTER `icon`;
