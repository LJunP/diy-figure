package com.diyfigure.canvas;

import com.diyfigure.auth.JwtInterceptor;
import com.diyfigure.canvas.dto.CanvasCreateRequest;
import com.diyfigure.canvas.dto.CanvasDetailResponse;
import com.diyfigure.canvas.dto.ChatRequest;
import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.entity.Canvas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 画布控制器
 *
 * 接口清单(对应 03-技术设计说明书.md 第 7.2 节):
 * - POST /api/series/{seriesId}/canvases  新建画布
 * - GET  /api/canvases/{id}               画布详情
 * - DELETE /api/canvases/{id}             删除画布
 * - POST /api/canvases/{id}/chat          AI 对话(SSE 流式返回)
 * - POST /api/canvases/{id}/finalize      画布定稿(触发 3D 模型生成)
 * - POST /api/canvases/{id}/reopen        重新打开已定稿画布
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CanvasController {

    private final CanvasService canvasService;

    /** 用于异步执行 SSE 对话的线程池 */
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    /**
     * 在系列下创建新画布
     */
    @PostMapping("/series/{seriesId}/canvases")
    public ApiResponse<Canvas> createCanvas(@PathVariable Long seriesId,
                                            @Valid @RequestBody CanvasCreateRequest body,
                                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(canvasService.createCanvas(seriesId, userId, body));
    }

    /**
     * 画布详情
     */
    @GetMapping("/canvases/{id}")
    public ApiResponse<CanvasDetailResponse> getCanvasDetail(@PathVariable Long id,
                                                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(canvasService.getCanvasDetail(id, userId));
    }

    /**
     * 删除画布
     */
    @DeleteMapping("/canvases/{id}")
    public ApiResponse<Void> deleteCanvas(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        canvasService.deleteCanvas(id, userId);
        return ApiResponse.success();
    }

    /**
     * AI 对话(SSE 流式返回)
     *
     * 前端使用 EventSource 或 fetch + ReadableStream 接收
     * SSE 事件类型:
     * - message: GPT-4o 的逐 token 回复
     * - image_generating: 开始生成概念图
     * - image_generated: 概念图生成成功(携带图片 URL)
     * - image_error: 概念图生成失败
     * - done: 对话完成
     */
    @PostMapping(value = "/canvases/{id}/chat", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chat(@PathVariable Long id,
                           @RequestBody ChatRequest body,
                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);

        // SSE 超时设置 5 分钟
        SseEmitter emitter = new SseEmitter(300_000L);

        // 异步执行对话,避免阻塞 HTTP 线程
        sseExecutor.execute(() -> {
            try {
                canvasService.chat(id, userId, body, emitter);
            } catch (Exception e) {
                log.error("AI 对话异常: canvasId={}", id, e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 画布定稿(触发 3D 模型生成)
     */
    @PostMapping("/canvases/{id}/finalize")
    public ApiResponse<Canvas> finalizeCanvas(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(canvasService.finalizeCanvas(id, userId));
    }

    /**
     * 重新打开已定稿画布(回到设计状态)
     */
    @PostMapping("/canvases/{id}/reopen")
    public ApiResponse<Canvas> reopenCanvas(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(canvasService.reopenCanvas(id, userId));
    }
}
