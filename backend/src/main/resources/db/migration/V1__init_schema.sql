-- V1: 初始表结构
--
-- 说明:
-- 1. 本脚本按实体映射生成,与 Hibernate 实体验证(ddl-auto=validate)保持一致,
--    新增列请写新的版本脚本,不要直接修改本文件(已执行过的版本不允许变更)。
-- 2. `order` 是 MySQL 保留字,必须加反引号。
-- 3. 现有开发库在首次启用 Flyway 时会被 baseline(见 application.yml 的 baseline-on-migrate),
--    不会重复执行本脚本。

CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('USER','ADMIN') NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  UNIQUE KEY `uk_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `series` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `spec_tier` enum('LIGHT','CLASSIC','COLLECTION') NOT NULL,
  `size_tier` varchar(50) NOT NULL,
  `design_status` enum('DESIGNING','READY_FOR_QUOTE') NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_series_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `canvas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `series_id` bigint NOT NULL,
  `status` enum('DESIGNING','FINALIZED') NOT NULL,
  `concept_image_urls` json DEFAULT NULL,
  `model_3d_url` varchar(500) DEFAULT NULL,
  `ai_conversation` json DEFAULT NULL,
  `finalized_at` datetime(6) DEFAULT NULL,
  `locked` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_canvas_series_id` (`series_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `series_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `order_type` enum('MAIN','REFILL') NOT NULL,
  `parent_order_id` bigint DEFAULT NULL,
  `status` enum('DRAFT_SUBMIT_PENDING','REVIEWING','REVIEW_REJECTED','QUOTED','CLOSED','LOTTERY_PENDING','LOTTERY_DONE','DEPOSIT_PENDING','IN_PRODUCTION','QC_PENDING','BALANCE_PENDING','SHIPPING_PENDING','SHIPPED','COMPLETED','CANCELLED') NOT NULL,
  `quoted_price` decimal(10,2) DEFAULT NULL,
  `expected_delivery_date` date DEFAULT NULL,
  `deposit_amount` decimal(10,2) DEFAULT NULL,
  `balance_amount` decimal(10,2) DEFAULT NULL,
  `production_started_at` datetime(6) DEFAULT NULL,
  `address_id` bigint DEFAULT NULL,
  `tracking_number` varchar(100) DEFAULT NULL,
  `tracking_company` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_user_id` (`user_id`),
  KEY `idx_order_series_id` (`series_id`),
  KEY `idx_order_status` (`status`),
  KEY `idx_order_parent_order_id` (`parent_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order_canvas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `canvas_id` bigint NOT NULL,
  `lottery_result` enum('SELECTED','NOT_SELECTED') NOT NULL,
  `refill_available_until` date DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_canvas_order_id` (`order_id`),
  KEY `idx_order_canvas_canvas_id` (`canvas_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `type` enum('DEPOSIT','BALANCE') NOT NULL,
  `channel` enum('WECHAT','ALIPAY') NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `status` enum('PENDING','SUCCESS','FAILED') NOT NULL,
  `channel_transaction_id` varchar(100) DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_payment_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `address` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `receiver_name` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `detail` varchar(500) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_address_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `review_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `reviewer_id` bigint NOT NULL,
  `result` enum('APPROVED','REJECTED') NOT NULL,
  `reject_reason` varchar(500) DEFAULT NULL,
  `reviewed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_review_log_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quality_check_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `result` enum('PASSED','FAILED') NOT NULL,
  `fail_reason` varchar(500) DEFAULT NULL,
  `checked_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_qc_log_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `cancellation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `stage_at_cancel` varchar(30) NOT NULL,
  `deposit_refund_amount` decimal(10,2) DEFAULT NULL,
  `deposit_penalty_amount` decimal(10,2) DEFAULT NULL,
  `cancelled_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cancellation_log_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `from_status` varchar(30) DEFAULT NULL,
  `to_status` varchar(30) NOT NULL,
  `operator_type` enum('USER','ADMIN','SYSTEM') NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_status_log_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `content` varchar(500) NOT NULL,
  `channel` enum('IN_APP','EMAIL') NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notification_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
