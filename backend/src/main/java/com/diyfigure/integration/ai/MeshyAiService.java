package com.diyfigure.integration.ai;

import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Meshy AI 3D 模型生成服务
 *
 * 图生 3D 参考模型(非直接可打印文件,供厂家原型师精修参考)
 *
 * 工作流程:
 * 1. 提交图片 URL → Meshy AI 创建生成任务,返回 task_id
 * 2. 轮询任务状态 → 完成后获取 .glb 文件 URL
 *
 * 注意:Meshy AI 是异步服务,需要轮询结果
 * 定稿时触发 3D 模型生成,生成完成后更新 canvas.model3d_url
 */
@Slf4j
@Service
public class MeshyAiService {

    @Value("${ai.meshy.api-key}")
    private String apiKey;

    @Value("${ai.meshy.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 提交图生 3D 任务
     *
     * @param imageUrl 概念图 URL(2D 三视图)
     * @return Meshy AI 任务 ID
     */
    public String createTextTo3dTask(String imageUrl) {
        try {
            if (!com.diyfigure.common.util.ExternalKeys.isConfigured(apiKey)) {
                log.warn("Meshy AI API Key 未配置,返回占位任务 ID");
                return "placeholder-task-id";
            }

            ObjectNode requestBody = objectMapper.createObjectNode();
            // Meshy AI image-to-3d API
            requestBody.put("image_url", imageUrl);
            requestBody.put("ai_model", "meshy-4");
            // 启用 PBR 纹理
            requestBody.put("enable_pbr", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/openapi/v1/image-to-3d"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Meshy AI 创建任务失败: status={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                        "3D 模型生成服务创建任务失败(HTTP " + response.statusCode() + ")");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String taskId = json.path("result").asText();
            log.info("Meshy AI 3D 生成任务已创建: taskId={}", taskId);
            return taskId;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Meshy AI 创建任务异常", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "3D 模型生成服务异常: " + e.getMessage());
        }
    }

    /**
     * 轮询 3D 生成任务状态
     *
     * @param taskId Meshy AI 任务 ID
     * @return 任务状态信息:{status, modelUrl}
     */
    public MeshyTaskResult getTaskStatus(String taskId) {
        try {
            if ("placeholder-task-id".equals(taskId)) {
                // 占位返回已完成状态 + 占位 URL
                return new MeshyTaskResult("COMPLETED",
                        "https://placehold.co/400x400/303030/ffffff/png?text=3D+Model+Ready");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/openapi/v1/image-to-3d/" + taskId))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Meshy AI 查询任务状态失败: status={}", response.statusCode());
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                        "3D 模型生成服务查询失败(HTTP " + response.statusCode() + ")");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String status = json.path("status").asText(); // PENDING / IN_PROGRESS / COMPLETED / FAILED
            String modelUrl = json.path("model_urls").path("glb").asText("");

            return new MeshyTaskResult(status, modelUrl);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Meshy AI 查询任务状态异常", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "3D 模型生成服务查询异常: " + e.getMessage());
        }
    }

    /**
     * 同步生成 3D 模型(轮询直到完成,最长等待 5 分钟)
     * 用于定稿时触发 3D 模型生成
     *
     * @param imageUrl 概念图 URL
     * @return 3D 模型 .glb 文件 URL
     */
    public String generate3dModelSync(String imageUrl) {
        String taskId = createTextTo3dTask(imageUrl);

        if ("placeholder-task-id".equals(taskId)) {
            log.warn("Meshy AI 未配置,返回占位 3D 模型 URL");
            return "https://placehold.co/400x400/303030/ffffff/png?text=3D+Model+Ready";
        }

        // 轮询任务状态,最长等待 5 分钟
        int maxAttempts = 60; // 60 次 × 5 秒 = 5 分钟
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "3D 模型生成被中断");
            }

            MeshyTaskResult result = getTaskStatus(taskId);
            log.info("Meshy AI 任务状态: taskId={}, status={}, attempt={}", taskId, result.status(), i + 1);

            if ("COMPLETED".equals(result.status())) {
                log.info("3D 模型生成完成: taskId={}, url={}", taskId, result.modelUrl());
                return result.modelUrl();
            }

            if ("FAILED".equals(result.status())) {
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "3D 模型生成失败");
            }
        }

        throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "3D 模型生成超时(5 分钟)");
    }

    /**
     * Meshy AI 任务状态结果
     */
    public record MeshyTaskResult(String status, String modelUrl) {}
}
