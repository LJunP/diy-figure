-- V5: 补上画布名称字段
--
-- 背景:CanvasCreateRequest 一直带着 name 字段(还标了 @NotBlank 校验),
-- 但 canvas 表压根没有这一列,于是用户填的名字被静默丢弃,
-- 订单详情、补购列表里到处硬编码 "画布 {id}" 当展示名。
--
-- 先把列加为可空并回填历史数据(名字与旧的展示文案保持一致,避免界面突然变空),
-- 再收紧为 NOT NULL,与实体上的 nullable = false 对齐。

ALTER TABLE `canvas`
  ADD COLUMN `name` varchar(100) DEFAULT NULL AFTER `series_id`;

UPDATE `canvas` SET `name` = CONCAT('角色 ', `id`) WHERE `name` IS NULL OR `name` = '';

ALTER TABLE `canvas`
  MODIFY COLUMN `name` varchar(100) NOT NULL;
