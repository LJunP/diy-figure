package com.diyfigure.payment.dto;

import com.diyfigure.common.enums.PaymentChannel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建支付请求 DTO
 */
@Data
public class PaymentCreateRequest {

    @NotNull(message = "支付渠道不能为空")
    private PaymentChannel channel; // WECHAT / ALIPAY
}
