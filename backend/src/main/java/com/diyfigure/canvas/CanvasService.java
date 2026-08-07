package com.diyfigure.canvas;

import com.diyfigure.canvas.dto.CanvasDetailResponse;
import com.diyfigure.canvas.dto.CanvasCreateRequest;
import com.diyfigure.canvas.dto.ChatRequest;
import com.diyfigure.common.enums.CanvasStatus;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.Canvas;
import com.diyfigure.entity.Series;
import com.diyfigure.integration.ai.MeshyAiService;
import com.diyfigure.integration.ai.OpenAiService;
import com.diyfigure.integration.oss.OssService;
import com.diyfigure.repository.CanvasRepository;
import com.diyfigure.series.SeriesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 画布服务
 *
 * 画布 = 一个原创角色的 AI 对话式设计
 * 核心能力:
 * 1. CRUD: 创建/查看/删除画布
 * 2. AI 对话: 与 GPT-4o 对话(SSE 流式返回),支持文字+图片输入
 * 3. AI 图片生成: DALL-E 3 根据对话描述生成 2D 概念图三视图
 * 4. 定稿: 用户确认设计完成,触发 3D 模型生成(Meshy AI)
 *
 * ★ 合规红线:GPT-4o 的 system prompt 预置过滤规则(见 OpenAiService)
 *   人工终审是最终防线(Phase 4),不依赖用户协议作为主要防御
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CanvasService {

    private final CanvasRepository canvasRepository;
    private final SeriesService seriesService;
    private final OpenAiService openAiService;
    private final OssService ossService;
    private final MeshyAiService meshyAiService;

    /**
     * 在系列下创建新画布
     */
    @Transactional
    public Canvas createCanvas(Long seriesId, Long userId, CanvasCreateRequest request) {
        // 校验系列归属权
        seriesService.getSeriesByIdAndUserId(seriesId, userId);

        Canvas canvas = Canvas.builder()
                .seriesId(seriesId)
                .status(CanvasStatus.DESIGNING)
                .conceptImageUrls(new ArrayList<>())
                .aiConversation(new ArrayList<>())
                .locked(false)
                .build();
        canvas = canvasRepository.save(canvas);
        log.info("创建画布: id={}, seriesId={}", canvas.getId(), seriesId);
        return canvas;
    }

    /**
     * 获取画布详情
     * 校验画布归属权(通过系列间接校验)
     */
    public CanvasDetailResponse getCanvasDetail(Long canvasId, Long userId) {
        Canvas canvas = getCanvasByIdAndUserId(canvasId, userId);
        return toDetailResponse(canvas);
    }

    /**
     * 删除画布
     * 锁定的画布不可删除
     */
    @Transactional
    public void deleteCanvas(Long canvasId, Long userId) {
        Canvas canvas = getCanvasByIdAndUserId(canvasId, userId);
        if (canvas.getLocked()) {
            throw new BusinessException(ResultCode.CANVAS_LOCKED);
        }
        canvasRepository.delete(canvas);
        log.info("删除画布: id={}", canvasId);
    }

    /**
     * AI 对话(SSE 流式返回)
     *
     * 流程:
     * 1. 将用户消息保存到对话记录
     * 2. 调用 GPT-4o 流式对话,SSE 逐 token 推送给前端
     * 3. 对话完成后,将完整回复保存到对话记录
     * 4. 如果 generateImage=true,调用 DALL-E 3 生成概念图
     *
     * @param canvasId 画布 ID
     * @param userId   用户 ID
     * @param request  对话请求
     * @param emitter  SSE 推送器
     */
    @Transactional
    public void chat(Long canvasId, Long userId, ChatRequest request, SseEmitter emitter) {
        Canvas canvas = getCanvasByIdAndUserId(canvasId, userId);

        // 锁定的画布不可对话
        if (canvas.getLocked()) {
            throw new BusinessException(ResultCode.CANVAS_LOCKED);
        }

        // 1. 保存用户消息到对话记录
        List<Map<String, Object>> conversation = canvas.getAiConversation();
        if (conversation == null) {
            conversation = new ArrayList<>();
        }

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", request.getMessage() != null ? request.getMessage() : "");
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            userMsg.put("images", request.getImageUrls());
        }
        userMsg.put("timestamp", LocalDateTime.now().toString());
        conversation.add(userMsg);

        // 2. 调用 GPT-4o 流式对话
        String aiReply = openAiService.chatStream(
                conversation,
                request.getMessage(),
                request.getImageUrls(),
                emitter
        );

        // 3. 保存 AI 回复到对话记录
        Map<String, Object> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", aiReply);
        assistantMsg.put("timestamp", LocalDateTime.now().toString());
        conversation.add(assistantMsg);

        canvas.setAiConversation(conversation);
        canvasRepository.save(canvas);

        // 4. 如果请求生成概念图
        if (Boolean.TRUE.equals(request.getGenerateImage())) {
            try {
                emitter.send(SseEmitter.event().name("image_generating").data("正在生成概念图..."));
                String imageUrl = openAiService.generateImage(aiReply);
                // 转存到 OSS(如果 OSS 已配置)
                String ossUrl = ossService.transferFromUrl(imageUrl, "canvases/generated");

                // 保存到画布的概念图列表
                List<String> imageUrls = canvas.getConceptImageUrls();
                if (imageUrls == null) {
                    imageUrls = new ArrayList<>();
                }
                imageUrls.add(ossUrl);
                canvas.setConceptImageUrls(imageUrls);
                canvasRepository.save(canvas);

                emitter.send(SseEmitter.event().name("image_generated").data(ossUrl));
                log.info("概念图生成成功: canvasId={}, imageUrl={}", canvasId, ossUrl);
            } catch (Exception e) {
                log.error("概念图生成失败: canvasId={}", canvasId, e);
                try {
                    emitter.send(SseEmitter.event().name("image_error").data("概念图生成失败: " + e.getMessage()));
                } catch (Exception ignored) {}
            }
        }

        try {
            emitter.send(SseEmitter.event().name("done").data("对话完成"));
        } catch (Exception ignored) {}
        emitter.complete();
    }

    /**
     * 画布定稿
     *
     * 定稿后:
     * 1. 画布状态改为 FINALIZED
     * 2. 触发 3D 模型生成(Meshy AI,使用第一张概念图)
     * 3. 检查系列是否达到档位要求的已定稿数量,更新设计状态
     *
     * ★ 已锁定的画布不可定稿(已在生产阶段)
     * ★ 定稿前必须有至少一张概念图
     *
     * @param canvasId 画布 ID
     * @param userId   用户 ID
     * @return 更新后的画布
     */
    @Transactional
    public Canvas finalizeCanvas(Long canvasId, Long userId) {
        Canvas canvas = getCanvasByIdAndUserId(canvasId, userId);

        if (canvas.getLocked()) {
            throw new BusinessException(ResultCode.CANVAS_LOCKED);
        }

        if (canvas.getStatus() == CanvasStatus.FINALIZED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "画布已定稿");
        }

        // 校验:定稿前必须有至少一张概念图
        if (canvas.getConceptImageUrls() == null || canvas.getConceptImageUrls().isEmpty()) {
            throw new BusinessException(ResultCode.CANVAS_NOT_FINALIZED,
                    "请先生成概念图后再定稿");
        }

        // 标记定稿
        canvas.setStatus(CanvasStatus.FINALIZED);
        canvas.setFinalizedAt(LocalDateTime.now());
        canvasRepository.save(canvas);

        // 触发 3D 模型生成(使用第一张概念图)
        // TODO: 正式环境应使用异步线程池,当前同步执行避免引入额外复杂度
        try {
            String firstImage = canvas.getConceptImageUrls().get(0);
            String model3dUrl = meshyAiService.generate3dModelSync(firstImage);
            canvas.setModel3dUrl(model3dUrl);
            canvasRepository.save(canvas);
            log.info("3D 模型生成完成: canvasId={}, modelUrl={}", canvasId, model3dUrl);
        } catch (Exception e) {
            // 3D 生成失败不阻塞定稿流程,用户可以后续重新生成
            log.warn("3D 模型生成失败(不阻塞定稿): canvasId={}, error={}", canvasId, e.getMessage());
        }

        // 检查系列是否达到档位要求
        seriesService.updateDesignStatusIfNeeded(canvas.getSeriesId());

        log.info("画布定稿: id={}, seriesId={}", canvasId, canvas.getSeriesId());
        return canvas;
    }

    /**
     * 重新打开已定稿画布(回到设计状态)
     * 仅在设计锁定前可用
     */
    @Transactional
    public Canvas reopenCanvas(Long canvasId, Long userId) {
        Canvas canvas = getCanvasByIdAndUserId(canvasId, userId);

        if (canvas.getLocked()) {
            throw new BusinessException(ResultCode.CANVAS_LOCKED);
        }

        if (canvas.getStatus() != CanvasStatus.FINALIZED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "画布未定稿,无需重新打开");
        }

        canvas.setStatus(CanvasStatus.DESIGNING);
        canvas.setFinalizedAt(null);
        canvasRepository.save(canvas);

        // 更新系列设计状态
        seriesService.updateDesignStatusIfNeeded(canvas.getSeriesId());

        log.info("画布重新打开: id={}", canvasId);
        return canvas;
    }

    // ===== 辅助方法 =====

    /**
     * 根据 ID 和 userId 查询画布(通过系列间接校验归属权)
     */
    public Canvas getCanvasByIdAndUserId(Long canvasId, Long userId) {
        Canvas canvas = canvasRepository.findById(canvasId)
                .orElseThrow(() -> new BusinessException(ResultCode.CANVAS_NOT_FOUND));

        // 通过系列校验归属权
        seriesService.getSeriesByIdAndUserId(canvas.getSeriesId(), userId);
        return canvas;
    }

    private CanvasDetailResponse toDetailResponse(Canvas canvas) {
        return CanvasDetailResponse.builder()
                .id(canvas.getId())
                .seriesId(canvas.getSeriesId())
                .status(canvas.getStatus().name())
                .conceptImageUrls(canvas.getConceptImageUrls())
                .model3dUrl(canvas.getModel3dUrl())
                .aiConversation(canvas.getAiConversation())
                .locked(canvas.getLocked())
                .createdAt(canvas.getCreatedAt())
                .updatedAt(canvas.getUpdatedAt())
                .finalizedAt(canvas.getFinalizedAt())
                .build();
    }
}
