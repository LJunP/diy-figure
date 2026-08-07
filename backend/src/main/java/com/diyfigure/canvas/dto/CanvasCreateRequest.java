package com.diyfigure.canvas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建画布请求 DTO
 * 画布 = 一个原创角色的 AI 对话式设计
 */
@Data
public class CanvasCreateRequest {

    @NotBlank(message = "画布名称不能为空")
    @Size(max = 100, message = "名称最多 100 个字符")
    private String name;
}
