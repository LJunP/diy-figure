package com.diyfigure.payment;

import com.diyfigure.auth.JwtInterceptor;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.common.util.ExternalKeys;
import com.diyfigure.common.util.HmacSigner;
import com.diyfigure.entity.Payment;
import com.diyfigure.payment.dto.PaymentCreateRequest;
import com.diyfigure.payment.dto.PaymentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付控制器
 *
 * 用户端:
 * - POST /api/payments/orders/{orderId}/deposit
 * - POST /api/payments/orders/{orderId}/balance
 * - GET  /api/payments/orders/{orderId}
 * - POST /api/payments/{paymentId}/simulate
 *
 * 支付平台回调(JWT 白名单,必须验签):
 * - POST /api/payments/callback/wechat
 * - POST /api/payments/callback/alipay
 *
 * 真实微信/支付宝 SDK 未接入前,验签使用 HMAC-SHA256 + 配置密钥。
 * 密钥未配置时失败关闭,绝不 ACK success。
 */
@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final String WECHAT_FAIL =
            "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[FAIL]]></return_msg></xml>";
    private static final String WECHAT_OK =
            "<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>";

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @PostMapping("/orders/{orderId}/deposit")
    public ApiResponse<PaymentResponse> createDepositPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentCreateRequest body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(paymentService.createDepositPayment(orderId, userId, body));
    }

    @PostMapping("/orders/{orderId}/balance")
    public ApiResponse<PaymentResponse> createBalancePayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentCreateRequest body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(paymentService.createBalancePayment(orderId, userId, body));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<List<Payment>> getPaymentHistory(
            @PathVariable Long orderId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(paymentService.getPaymentHistory(orderId, userId));
    }

    @PostMapping("/{paymentId}/simulate")
    public ApiResponse<PaymentResponse> simulatePaymentSuccess(
            @PathVariable Long paymentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(paymentService.simulatePaymentSuccess(paymentId, userId));
    }

    @PostMapping(value = "/callback/wechat", produces = "application/xml;charset=UTF-8")
    public String wechatCallback(@RequestBody(required = false) String body) {
        if (!ExternalKeys.isConfigured(paymentService.wechatCallbackKey())) {
            log.warn("收到微信支付回调,但未配置 WECHAT_PAY_API_KEY,已拒绝");
            return WECHAT_FAIL;
        }
        try {
            Map<String, String> fields = parseCallbackBody(body);
            if (!HmacSigner.matches(paymentService.wechatCallbackKey(),
                    HmacSigner.canonicalQuery(fields), fields.get("sign"))) {
                log.warn("微信支付回调验签失败");
                return WECHAT_FAIL;
            }
            applyCallback(fields);
            return WECHAT_OK;
        } catch (Exception e) {
            log.warn("微信支付回调处理失败: {}", e.getMessage());
            return WECHAT_FAIL;
        }
    }

    @PostMapping("/callback/alipay")
    public String alipayCallback(@RequestParam Map<String, String> params,
                                 @RequestBody(required = false) String body) {
        if (!ExternalKeys.isConfigured(paymentService.alipayCallbackKey())) {
            log.warn("收到支付宝回调,但未配置 ALIPAY_PAY_API_KEY,已拒绝");
            return "fail";
        }
        try {
            Map<String, String> fields = params != null && !params.isEmpty()
                    ? new LinkedHashMap<>(params)
                    : parseCallbackBody(body);
            if (!HmacSigner.matches(paymentService.alipayCallbackKey(),
                    HmacSigner.canonicalQuery(fields), fields.get("sign"))) {
                log.warn("支付宝回调验签失败");
                return "fail";
            }
            applyCallback(fields);
            return "success";
        } catch (Exception e) {
            log.warn("支付宝回调处理失败: {}", e.getMessage());
            return "fail";
        }
    }

    private void applyCallback(Map<String, String> fields) {
        String outTradeNo = first(fields, "out_trade_no", "paymentId");
        String txn = first(fields, "transaction_id", "trade_no", "channelTransactionId");
        String amount = first(fields, "amount", "total_amount");
        if (outTradeNo == null || amount == null) {
            throw new BusinessException(com.diyfigure.common.response.ResultCode.BAD_REQUEST, "回调缺少支付单号或金额");
        }
        paymentService.handleGatewayCallback(Long.parseLong(outTradeNo), txn, new BigDecimal(amount));
    }

    private Map<String, String> parseCallbackBody(String body) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (body == null || body.isBlank()) {
            return fields;
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                node.fields().forEachRemaining(e -> {
                    if (e.getValue() != null && !e.getValue().isNull()) {
                        fields.put(e.getKey(), e.getValue().asText());
                    }
                });
                return fields;
            } catch (Exception e) {
                throw new BusinessException(com.diyfigure.common.response.ResultCode.BAD_REQUEST, "回调 JSON 无法解析");
            }
        }
        // 简易 XML:<tag>value</tag> 或 <tag><![CDATA[value]]></tag>
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<(\\w+)>\\s*(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?\\s*</\\1>")
                .matcher(trimmed);
        while (m.find()) {
            fields.put(m.group(1), m.group(2));
        }
        return fields;
    }

    private static String first(Map<String, String> fields, String... keys) {
        for (String key : keys) {
            String v = fields.get(key);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
