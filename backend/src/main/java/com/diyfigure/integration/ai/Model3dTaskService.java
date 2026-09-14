package com.diyfigure.integration.ai;

import com.diyfigure.common.enums.Model3dStatus;
import com.diyfigure.entity.Canvas;
import com.diyfigure.repository.CanvasRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 3D 参考模型生成任务(异步)
 *
 * 为什么改异步:
 * 定稿接口原本同步轮询 Meshy,最长 5 分钟。前端 Axios 超时是 30 秒,
 * 真实密钥下几乎必然超时,而且定稿请求会被长时间占用。
 *
 * 现在的流程:
 * 1. 定稿 → 画布标记 PENDING,立即返回
 * 2. 定时任务每 15 秒推进:
 *    - PENDING   → 创建 Meshy 任务,记录 taskId,转 PROCESSING
 *    - PROCESSING→ 轮询任务结果,转 SUCCESS(写入模型 URL)或 FAILED
 * 3. 失败后由用户或运营调用重试接口,重新置为 PENDING
 *
 * 任务状态落库,所以重启后不会丢任务,也不会重复提交。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Model3dTaskService {

    /** 未配置 Meshy 时的占位返回,与 MeshyAiService 约定一致 */
    private static final String PLACEHOLDER_TASK_ID = "placeholder-task-id";
    private static final String PLACEHOLDER_MODEL_URL =
            "https://placehold.co/400x400/303030/ffffff/png?text=3D+Model+Ready";

    /** 单次调度最多提交的生成任务数,避免瞬时打满外部配额 */
    private static final int MAX_SUBMIT_PER_RUN = 3;
    /** 单次调度最多轮询的任务数 */
    private static final int MAX_POLL_PER_RUN = 10;

    private final CanvasRepository canvasRepository;
    private final MeshyAiService meshyAiService;

    /** 调度总开关,测试环境置 false(见 src/test/resources/application-test.yml) */
    @org.springframework.beans.factory.annotation.Value("${diy.scheduling.enabled:true}")
    private boolean schedulingEnabled;

    /**
     * 入队:定稿时调用,只改状态不调用外部接口,保证定稿接口能立刻返回
     */
    public void enqueue(Canvas canvas) {
        canvas.setModel3dStatus(Model3dStatus.PENDING);
        canvas.setModel3dTaskId(null);
    }

    /**
     * 重新入队(受控重试):仅允许在失败后调用
     */
    public void requeue(Canvas canvas) {
        if (canvas.getModel3dStatus() != Model3dStatus.FAILED) {
            return;
        }
        enqueue(canvas);
        canvasRepository.save(canvas);
        log.info("3D 生成任务已重新入队: canvasId={}", canvas.getId());
    }

    /**
     * 清空 3D 状态(画布重新打开时调用)
     *
     * 画布定稿会生成 3D 模型;如果用户随后「重新打开」回到设计态,设计已经变了,
     * 旧的模型 URL 就不再对应现在的设计。不清掉的话画布页会一直显示上一版的模型,
     * 而且状态停在 SUCCESS 会让重试接口拒绝调用(它只接受 FAILED)。
     */
    public void reset(Canvas canvas) {
        canvas.setModel3dStatus(Model3dStatus.NONE);
        canvas.setModel3dTaskId(null);
        canvas.setModel3dUrl(null);
    }

    @Scheduled(fixedDelay = 15000, initialDelay = 10000)
    public void process() {
        // 总开关。注意不能用 spring.task.scheduling.enabled —— 那不是 Spring Boot 的属性,
        // 设了也不生效(调度线程照样起)。集成测试靠这个开关关掉轮询,
        // 否则定时任务会在 Flyway clean 重建表的过程中查询 canvas 表并报错。
        if (!schedulingEnabled) {
            return;
        }
        try {
            submitPending();
        } catch (Exception e) {
            log.error("提交 3D 生成任务失败(下个周期重试): {}", e.getMessage());
        }
        try {
            pollProcessing();
        } catch (Exception e) {
            log.error("轮询 3D 生成任务失败(下个周期重试): {}", e.getMessage());
        }
    }

    private void submitPending() {
        List<Canvas> pending = canvasRepository.findByModel3dStatus(Model3dStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }

        int submitted = 0;
        for (Canvas canvas : pending) {
            if (submitted >= MAX_SUBMIT_PER_RUN) {
                break;
            }
            try {
                String imageUrl = (canvas.getConceptImageUrls() != null && !canvas.getConceptImageUrls().isEmpty())
                        ? canvas.getConceptImageUrls().get(0)
                        : null;
                if (imageUrl == null) {
                    canvas.setModel3dStatus(Model3dStatus.FAILED);
                    canvasRepository.save(canvas);
                    log.warn("画布没有概念图,3D 生成无法进行: canvasId={}", canvas.getId());
                    continue;
                }

                String taskId = meshyAiService.createTextTo3dTask(imageUrl);
                if (PLACEHOLDER_TASK_ID.equals(taskId)) {
                    // 未配置 Meshy:直接给出占位结果,保持与旧行为一致
                    canvas.setModel3dStatus(Model3dStatus.SUCCESS);
                    canvas.setModel3dUrl(PLACEHOLDER_MODEL_URL);
                    canvasRepository.save(canvas);
                    log.info("Meshy 未配置,3D 使用占位结果: canvasId={}", canvas.getId());
                    continue;
                }

                canvas.setModel3dTaskId(taskId);
                canvas.setModel3dStatus(Model3dStatus.PROCESSING);
                canvasRepository.save(canvas);
                submitted++;
                log.info("3D 生成任务已提交: canvasId={}, taskId={}", canvas.getId(), taskId);
            } catch (Exception e) {
                canvas.setModel3dStatus(Model3dStatus.FAILED);
                canvasRepository.save(canvas);
                log.error("提交 3D 任务失败: canvasId={}, error={}", canvas.getId(), e.getMessage());
            }
        }
    }

    private void pollProcessing() {
        List<Canvas> processing = canvasRepository.findByModel3dStatus(Model3dStatus.PROCESSING);
        if (processing.isEmpty()) {
            return;
        }

        int polled = 0;
        for (Canvas canvas : processing) {
            if (polled >= MAX_POLL_PER_RUN) {
                break;
            }
            try {
                MeshyAiService.MeshyTaskResult result = meshyAiService.getTaskStatus(canvas.getModel3dTaskId());

                if ("COMPLETED".equals(result.status()) && result.modelUrl() != null) {
                    canvas.setModel3dUrl(result.modelUrl());
                    canvas.setModel3dStatus(Model3dStatus.SUCCESS);
                    canvasRepository.save(canvas);
                    log.info("3D 模型生成完成: canvasId={}, url={}", canvas.getId(), result.modelUrl());
                } else if ("FAILED".equals(result.status())) {
                    canvas.setModel3dStatus(Model3dStatus.FAILED);
                    canvasRepository.save(canvas);
                    log.warn("3D 模型生成失败: canvasId={}, taskId={}", canvas.getId(), canvas.getModel3dTaskId());
                }
                polled++;
            } catch (Exception e) {
                // 单次查询失败不急着判死,下个周期继续尝试
                log.warn("轮询 3D 任务异常,下个周期重试: canvasId={}, error={}", canvas.getId(), e.getMessage());
            }
        }
    }
}
