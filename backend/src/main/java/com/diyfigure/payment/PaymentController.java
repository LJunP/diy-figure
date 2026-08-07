package com.diyfigure.payment;

import com.diyfigure.auth.JwtInterceptor;
import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.entity.Payment;
import com.diyfigure.payment.dto.PaymentCreateRequest;
import com.diyfigure.payment.dto.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付控制器
 *
 * 用户端接口:
 * - POST /api/orders/{orderId}/payments/deposit  创建定金支付
 * - POST /api/orders/{orderId}/payments/balance   创建尾款支付
 * - GET  /api/orders/{orderId}/payments           查询支付记录
 * - POST /api/payments/{paymentId}/simulate       模拟支付成功(开发环境)
 *
 * 支付回调接口(真实环境):
 * - POST /api/payments/callback/wechat            微信支付异步回调
 * - POST /api/payments/callback/alipay            支付宝异步回调
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 创建定金支付单
     */
    @PostMapping("/orders/{orderId}/deposit")
    public ApiResponse<PaymentResponse> createDepositPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentCreateRequest body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(paymentService.createDepositPayment(orderId, userId, body));
    }

    /**
     * 创建尾款支付单
     */
    @PostMapping("/orders/{orderId}/balance")
    public ApiResponse<PaymentResponse> createBalancePayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentCreateRequest body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(paymentService.createBalancePayment(orderId, userId, body));
    }

    /**
     * 查询订单的支付记录
     */
    @GetMapping("/orders/{orderId}")
    public ApiResponse<List<Payment>> getPaymentHistory(
            @PathVariable Long orderId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(paymentService.getPaymentHistory(orderId, userId));
    }

    /**
     * 模拟支付成功(开发环境)
     *
     * 真实环境中此接口由支付网关异步回调替代
     */
    @PostMapping("/{paymentId}/simulate")
    public ApiResponse<PaymentResponse> simulatePaymentSuccess(
            @PathVariable Long paymentId) {
        return ApiResponse.success(paymentService.simulatePaymentSuccess(paymentId));
    }

    // ===== 真实支付回调接口(预留,待 SDK 接入后实现) =====

    /**
     * 微信支付异步回调
     *
     * TODO: 接入微信支付 SDK 后实现验签 + 解析回调数据
     */
    @PostMapping("/callback/wechat")
    public String wechatCallback(@RequestBody String body) {
        // TODO: 实现微信支付回调验签
        // 1. 解析 XML 数据
        // 2. 验证签名
        // 3. 提取 out_trade_no(paymentId) 和 transaction_id
        // 4. 调用 paymentService.simulatePaymentSuccess(paymentId)
        log.info("收到微信支付回调: {}", body);
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>";
    }

    /**
     * 支付宝异步回调
     *
     * TODO: 接入支付宝 SDK 后实现验签 + 解析回调数据
     */
    @PostMapping("/callback/alipay")
    public String alipayCallback(@RequestParam java.util.Map<String, String> params) {
        // TODO: 实现支付宝回调验签
        // 1. 调用 AlipaySignature.rsaCheckV1 验签
        // 2. 提取 out_trade_no(paymentId) 和 trade_no
        // 3. 调用 paymentService.simulatePaymentSuccess(paymentId)
        log.info("收到支付宝回调: {}", params);
        return "success";
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentController.class);
}
