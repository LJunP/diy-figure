package com.diyfigure.refill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发起补购请求 DTO
 */
@Data
public class RefillRequest {

    @NotNull(message = "画布 ID 不能为空")
    private Long canvasId;

    @NotNull(message = "支付渠道不能为空")
    private String channel; // WECHAT / ALIPAY
}
