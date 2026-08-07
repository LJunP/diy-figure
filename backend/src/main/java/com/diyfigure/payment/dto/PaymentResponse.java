package com.diyfigure.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付响应 DTO
 *
 * 返回支付参数给前端:
 * - 真实环境:微信/支付宝 SDK 返回的支付参数(prepay_id, code_url 等)
 * - 模拟环境:返回模拟支付页面 URL 和支付单号
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private Long orderId;
    private String type;         // DEPOSIT / BALANCE
    private String channel;      // WECHAT / ALIPAY
    private BigDecimal amount;
    private String status;       // PENDING / SUCCESS / FAILED

    /** 支付参数(真实环境:微信 prepay_id / 支付宝跳转URL;模拟环境:模拟支付URL) */
    private String payUrl;

    /** 模拟支付模式标记(前端据此显示模拟支付按钮) */
    private Boolean mockMode;

    private LocalDateTime createdAt;
}
