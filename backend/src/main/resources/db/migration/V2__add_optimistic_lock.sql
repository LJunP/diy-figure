-- V2: 为关键实体增加乐观锁版本号
--
-- 背景:抽签、支付回调、补购去重之前只靠应用层"先查后写"判断,
-- 两个并发请求可能读到同一份旧状态并各自写回。加 @Version 后,
-- 后提交的一方会触发 ObjectOptimisticLockingFailureException。
--
-- 注意:这里用 NOT NULL DEFAULT 0,已有数据会被填成 0,Hibernate 会自行递增。

ALTER TABLE `order`        ADD COLUMN `version` bigint NOT NULL DEFAULT 0;
ALTER TABLE `order_canvas` ADD COLUMN `version` bigint NOT NULL DEFAULT 0;
ALTER TABLE `payment`      ADD COLUMN `version` bigint NOT NULL DEFAULT 0;
ALTER TABLE `canvas`       ADD COLUMN `version` bigint NOT NULL DEFAULT 0;
ALTER TABLE `series`       ADD COLUMN `version` bigint NOT NULL DEFAULT 0;
