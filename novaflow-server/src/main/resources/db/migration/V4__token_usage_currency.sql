-- token_usage 增加计费币种

ALTER TABLE `token_usage`
    ADD COLUMN `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '计费币种：CNY/USD' AFTER `cost`;
