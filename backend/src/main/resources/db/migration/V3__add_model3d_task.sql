-- V3: 3D 模型生成任务异步化
--
-- 定稿时不再同步轮询 Meshy(最长 5 分钟,会造成前端 30 秒超时)。
-- 改为在画布上记录任务 ID 与状态,由后台调度推进,失败可受控重试。

-- model_3d_status 必须与 Hibernate 期望的类型一致:
-- Hibernate 6 对 MySQL 的 @Enumerated(EnumType.STRING) 生成的是 enum(...) 而不是 varchar,
-- 写成 varchar 时生产环境 ddl-auto=validate 会直接启动失败。
ALTER TABLE `canvas`
  ADD COLUMN `model_3d_task_id` varchar(100) DEFAULT NULL,
  ADD COLUMN `model_3d_status` enum('NONE','PENDING','PROCESSING','SUCCESS','FAILED') NOT NULL DEFAULT 'NONE';

-- 调度器按状态捞取待处理任务
CREATE INDEX `idx_canvas_model3d_status` ON `canvas` (`model_3d_status`);
