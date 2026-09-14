-- V6: 邮箱验证 / 忘记密码 / 支付退款记录 / 抽奖未决 / 默认地址
--
-- 已执行过的版本文件不可改;本脚本只追加列和表。
-- 存量用户视为已验证、已启用,避免演示账号被锁死。
-- 新注册由应用层把 email_verified 写成 0。

ALTER TABLE `user`
  ADD COLUMN `email_verified` bit(1) NOT NULL DEFAULT b'1' AFTER `role`,
  ADD COLUMN `enabled` bit(1) NOT NULL DEFAULT b'1' AFTER `email_verified`;

CREATE TABLE IF NOT EXISTS `auth_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `purpose` enum('EMAIL_VERIFY','PASSWORD_RESET') NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `consumed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_token_hash` (`token_hash`),
  KEY `idx_auth_token_user_purpose` (`user_id`, `purpose`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `payment`
  ADD COLUMN `refunded_at` datetime(6) DEFAULT NULL,
  ADD COLUMN `refund_amount` decimal(10,2) DEFAULT NULL;

-- 抽奖前不再默认 SELECTED,避免详情页把所有角色显示成中签
ALTER TABLE `order_canvas`
  MODIFY COLUMN `lottery_result` enum('UNDECIDED','SELECTED','NOT_SELECTED') NOT NULL;

ALTER TABLE `address`
  ADD COLUMN `is_default` bit(1) NOT NULL DEFAULT b'0' AFTER `detail`;
