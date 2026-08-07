package com.diyfigure.canvas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 对话请求 DTO
 * 用户在画布中发送消息给 GPT-4o,支持纯文字或文字+图片
 */
@Data
public class ChatRequest {

    /** 用户输入的文字消息 */
    private String message;

    /** 参考图 URL 列表(已上传到 OSS 的图片地址,作为 GPT-4o 的图片输入) */
    private java.util.List<String> imageUrls;

    /**
     * 是否同时请求生成概念图(调用 DALL-E 3)
     * true: AI 回复对话 + 生成 2D 三视图概念图
     * false: 仅对话
     */
    private Boolean generateImage = false;
}
