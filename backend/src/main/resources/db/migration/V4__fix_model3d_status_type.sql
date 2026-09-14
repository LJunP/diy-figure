-- V4: 修正已迁移库上 model_3d_status 的列类型
--
-- V3 的初版把 model_3d_status 建成了 varchar(20),而 Hibernate 6 对 MySQL 的
-- @Enumerated(EnumType.STRING) 期望 enum(...)。生产环境 ddl-auto=validate 会因此
-- 启动失败,所以 V3 已改为直接建 enum;本脚本负责把「已经跑过旧版 V3」的库纠正过来。
--
-- 新库上 V3 已经建对,本语句是幂等的 no-op。
ALTER TABLE `canvas`
  MODIFY COLUMN `model_3d_status` enum('NONE','PENDING','PROCESSING','SUCCESS','FAILED') NOT NULL DEFAULT 'NONE';
