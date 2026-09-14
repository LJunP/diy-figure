package com.diyfigure.integration.ai;

import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI AI 服务
 *
 * 封装 GPT-4o 对话 + DALL-E 3 图片生成
 *
 * ★ 合规红线:system prompt 预置过滤规则,禁止生成可辨识真实人物/已有 IP 角色
 *   这是 AI 自审层面的防线,人工终审是最终防线
 *
 * 技术说明:
 * - 对话使用 SSE 流式返回,提升用户体验(逐字输出)
 * - 图片输入:用户上传参考图 → base64 编码 → 发给 GPT-4o vision
 * - 图片生成:DALL-E 3 根据对话描述生成 2D 概念图三视图
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.base-url}")
    private String baseUrl;

    @Value("${ai.openai.chat-model}")
    private String chatModel;

    @Value("${ai.openai.image-model}")
    private String imageModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * GPT-4o 合规 system prompt
     * ★ 预置过滤规则:禁止生成可辨识真实人物/已有 IP 角色
     * 引导用户转向原创方向而非直接拒绝
     */
    private static final String SYSTEM_PROMPT = """
            你是一位专业的盲盒手办角色设计师,帮助用户设计原创角色形象。

            【合规红线 - 必须严格遵守】
            1. 禁止生成任何可辨识的真实公众人物、明星、政治人物形象
            2. 禁止模仿已有版权 IP 角色(如迪士尼、漫威、任天堂、宝可梦等)
            3. 当用户要求模仿上述内容时,礼貌引导转向原创方向,而非直接拒绝
            4. 所有设计必须是原创角色,可以参考风格但不能复制具体角色

            【设计指导】
            1. 用户描述角色概念后,你需要提供详细的角色设计描述,包括:
               - 角色外观(发型、面部特征、服装、配饰)
               - 角色色彩方案
               - 角色性格和背景故事(简短)
               - 适合盲盒手办的 Q 版或写实比例建议
            2. 当用户满意设计后,准备生成概念图三视图(正面、侧面、背面)
            3. 保持设计的一致性,多次迭代时记住之前的设计元素

            请用中文回复,保持专业但友好的语气。
            """;

    /**
     * GPT-4o 对话(SSE 流式返回)
     *
     * @param conversationHistory 历史对话记录(canvas.ai_conversation)
     * @param userMessage 用户当前消息
     * @param imageUrls 用户上传的参考图 URL 列表(可选,GPT-4o vision)
     * @param sseEmitter SSE 推送器,逐 token 推送给前端
     * @return GPT-4o 的完整回复文本
     */
    public String chatStream(List<Map<String, Object>> conversationHistory,
                             String userMessage,
                             List<String> imageUrls,
                             SseEmitter sseEmitter) {
        // Demo 模式:API Key 未配置时返回模拟回复
        if (!com.diyfigure.common.util.ExternalKeys.isConfigured(apiKey)) {
            log.warn("OpenAI API Key 未配置,返回 Demo 模拟回复");
            String mockReply = generateMockReply(userMessage, imageUrls);
            // 逐 token 推送,模拟流式效果
            for (String token : mockReply.split("(?<=.)")) {
                try {
                    sseEmitter.send(SseEmitter.event().data(token));
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.debug("SSE 推送异常: {}", e.getMessage());
                }
            }
            return mockReply;
        }

        try {
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", chatModel);
            requestBody.put("stream", true);
            requestBody.put("max_tokens", 2000);

            ArrayNode messages = requestBody.putArray("messages");

            // 1. system prompt
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);

            // 2. 历史对话(最多保留最近 20 轮,避免 token 超限)
            int startIdx = Math.max(0, conversationHistory.size() - 20);
            for (int i = startIdx; i < conversationHistory.size(); i++) {
                Map<String, Object> hist = conversationHistory.get(i);
                ObjectNode histMsg = messages.addObject();
                histMsg.put("role", (String) hist.get("role"));

                // 如果有图片,用多模态格式
                Object imagesObj = hist.get("images");
                if (imagesObj instanceof List && !((List<?>) imagesObj).isEmpty()) {
                    ArrayNode content = histMsg.putArray("content");
                    // 文字部分
                    ObjectNode textPart = content.addObject();
                    textPart.put("type", "text");
                    textPart.put("text", (String) hist.getOrDefault("content", ""));
                    // 图片部分
                    for (Object imgUrl : (List<?>) imagesObj) {
                        ObjectNode imagePart = content.addObject();
                        imagePart.put("type", "image_url");
                        ObjectNode imageUrl = imagePart.putObject("image_url");
                        imageUrl.put("url", (String) imgUrl);
                    }
                } else {
                    histMsg.put("content", (String) hist.getOrDefault("content", ""));
                }
            }

            // 3. 当前用户消息(支持图片输入)
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            if (imageUrls != null && !imageUrls.isEmpty()) {
                ArrayNode content = userMsg.putArray("content");
                ObjectNode textPart = content.addObject();
                textPart.put("type", "text");
                textPart.put("text", userMessage != null ? userMessage : "请根据参考图设计角色");
                for (String imgUrl : imageUrls) {
                    ObjectNode imagePart = content.addObject();
                    imagePart.put("type", "image_url");
                    ObjectNode imageUrlObj = imagePart.putObject("image_url");
                    imageUrlObj.put("url", imgUrl);
                }
            } else {
                userMsg.put("content", userMessage);
            }

            // 发送流式请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofLines()
            );

            if (response.statusCode() != 200) {
                String errorBody = response.body().reduce("", (a, b) -> a + b);
                log.error("GPT-4o 调用失败: status={}, body={}", response.statusCode(), errorBody);
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                        "AI 对话服务调用失败(HTTP " + response.statusCode() + ")");
            }

            // 解析 SSE 流,逐 token 推送
            StringBuilder fullResponse = new StringBuilder();
            response.body().forEach(line -> {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        return;
                    }
                    try {
                        JsonNode json = objectMapper.readTree(data);
                        JsonNode delta = json.path("choices").path(0).path("delta").path("content");
                        if (!delta.isMissingNode() && !delta.isNull()) {
                            String token = delta.asText();
                            fullResponse.append(token);
                            // 推送给前端
                            try {
                                sseEmitter.send(SseEmitter.event().data(token));
                            } catch (Exception e) {
                                log.debug("SSE 推送异常(客户端可能已断开): {}", e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        log.debug("解析 SSE 数据异常: {}", e.getMessage());
                    }
                }
            });

            return fullResponse.toString();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GPT-4o 对话异常", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 对话服务异常: " + e.getMessage());
        }
    }

    /**
     * GPT-4o 对话(非流式,用于无 SSE 场景)
     */
    public String chat(List<Map<String, Object>> conversationHistory,
                       String userMessage,
                       List<String> imageUrls) {
        // Demo 模式:API Key 未配置时返回模拟回复
        if (!com.diyfigure.common.util.ExternalKeys.isConfigured(apiKey)) {
            log.warn("OpenAI API Key 未配置,返回 Demo 模拟回复");
            return generateMockReply(userMessage, imageUrls);
        }
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", chatModel);
            requestBody.put("max_tokens", 2000);

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);

            int startIdx = Math.max(0, conversationHistory.size() - 20);
            for (int i = startIdx; i < conversationHistory.size(); i++) {
                Map<String, Object> hist = conversationHistory.get(i);
                ObjectNode histMsg = messages.addObject();
                histMsg.put("role", (String) hist.get("role"));
                histMsg.put("content", (String) hist.getOrDefault("content", ""));
            }

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            if (imageUrls != null && !imageUrls.isEmpty()) {
                ArrayNode content = userMsg.putArray("content");
                ObjectNode textPart = content.addObject();
                textPart.put("type", "text");
                textPart.put("text", userMessage != null ? userMessage : "请根据参考图设计角色");
                for (String imgUrl : imageUrls) {
                    ObjectNode imagePart = content.addObject();
                    imagePart.put("type", "image_url");
                    ObjectNode imageUrlObj = imagePart.putObject("image_url");
                    imageUrlObj.put("url", imgUrl);
                }
            } else {
                userMsg.put("content", userMessage);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("GPT-4o 调用失败: status={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                        "AI 对话服务调用失败(HTTP " + response.statusCode() + ")");
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("choices").path(0).path("message").path("content").asText();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GPT-4o 对话异常", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 对话服务异常: " + e.getMessage());
        }
    }

    /**
     * DALL-E 3 生成概念图
     *
     * 根据 GPT-4o 的设计描述生成 2D 概念图三视图
     * 开发阶段如果 API Key 未配置,返回占位图 URL
     *
     * @param prompt 图片生成提示词(基于对话内容构建)
     * @return 生成的图片 URL
     */
    public String generateImage(String prompt) {
        try {
            // 如果 API Key 是占位值,返回占位图
            if (!com.diyfigure.common.util.ExternalKeys.isConfigured(apiKey)) {
                log.warn("OpenAI API Key 未配置,返回占位图 URL");
                return "https://placehold.co/512x512/3D2B5F/FFD700/png?text=AI+Concept+Art";
            }

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", imageModel);
            // DALL-E 3 prompt 前缀:强调原创、三视图风格
            String enhancedPrompt = "Original blind box figurine concept art, " + prompt +
                    ". Three-view design (front, side, back), high quality, detailed, " +
                    "suitable for 3D modeling reference. NOT based on any real person or existing IP.";
            requestBody.put("prompt", enhancedPrompt);
            requestBody.put("n", 1);
            requestBody.put("size", "1024x1024");
            requestBody.put("quality", "hd");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/images/generations"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("DALL-E 3 调用失败: status={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                        "AI 图片生成失败(HTTP " + response.statusCode() + ")");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String imageUrl = json.path("data").path(0).path("url").asText();
            log.info("DALL-E 3 图片生成成功: {}", imageUrl);
            return imageUrl;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DALL-E 3 图片生成异常", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 图片生成异常: " + e.getMessage());
        }
    }

    /**
     * Demo 模式:生成模拟 AI 回复
     * 当 OpenAI API Key 未配置时,返回一段合理的角色设计描述
     */
    private String generateMockReply(String userMessage, List<String> imageUrls) {
        StringBuilder sb = new StringBuilder();
        sb.append("你好!我是 AI 角色设计师。根据你的描述「");
        sb.append(userMessage != null ? userMessage : "未提供描述");
        sb.append("」,我为你设计了以下原创角色:\n\n");

        sb.append("【角色名称】星光精灵\n\n");
        sb.append("【外观设计】\n");
        sb.append("- 发型:银白色长发,发梢带有淡蓝色渐变,头顶有一对精致的精灵耳\n");
        sb.append("- 面部:大眼睛,琥珀色瞳孔,面带温柔微笑,脸颊有星形印记\n");
        sb.append("- 服装:深紫色魔法师长袍,衣摆点缀金色星辰纹路,腰间系有月牙形腰饰\n");
        sb.append("- 配饰:右手持星空法杖,左手腕戴有水晶手链\n\n");

        sb.append("【色彩方案】\n");
        sb.append("- 主色:深紫(#3D2B5F)\n");
        sb.append("- 辅色:银白(#E8E8E8)、星光金(#FFD700)\n");
        sb.append("- 点缀:淡蓝(#AEDFF2)\n\n");

        sb.append("【角色背景】\n");
        sb.append("星光精灵是夜空中最纯净的星光凝聚而成的生命体,守护着梦境与希望的交界之地。");
        sb.append("她性格温柔但内心坚定,用星空法杖为迷路的人指引方向。\n\n");

        sb.append("【盲盒建议】\n");
        sb.append("推荐 Q 版比例(头身比约 1:2.5),适合 10cm 高度的收藏级盲盒。");
        sb.append("可设计为坐姿形态,裙摆散开形成底座,既美观又便于量产。\n\n");

        if (imageUrls != null && !imageUrls.isEmpty()) {
            sb.append("我已注意到你上传的参考图,在正式生成概念图时会融入参考图中的风格元素。\n\n");
        }

        sb.append("如果你对以上设计满意,可以勾选「同时生成概念图」让我画出来,然后点击「定稿」。");
        sb.append("如需调整,请告诉我你想修改的部分!");

        return sb.toString();
    }
}
