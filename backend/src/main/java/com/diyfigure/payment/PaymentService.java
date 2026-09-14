package com.diyfigure.payment;

import com.diyfigure.common.enums.*;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.*;
import com.diyfigure.order.service.OrderStateMachineService;
import com.diyfigure.payment.dto.*;
import com.diyfigure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付服务
 *
 * 核心职责:
 * 1. 创建支付单(定金/尾款):校验订单状态,生成 Payment 记录
 * 2. 模拟支付回调:开发环境无真实支付网关,提供模拟回调接口
 * 3. 支付成功后联动订单状态:
 *    - 定金支付成功: DEPOSIT_PENDING → IN_PRODUCTION
 *    - 尾款支付成功: BALANCE_PENDING → SHIPPING_PENDING
 *
 * ★ 关键约束:
 * - 定金支付成功后,所有画布锁定(locked=true),不可再编辑
 * - 每笔订单同一时间只能有一笔 PENDING 状态的支付单
 * - 支付回调必须幂等:同一 paymentId 重复回调不应产生副作用
 *
 * 真实环境集成:
 * - 微信支付:POST /api/payments/callback/wechat 接收微信异步通知
 * - 支付宝:POST /api/payments/callback/alipay 接收支付宝异步通知
 * - 回调验签通过后调用 handlePaymentSuccess()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderCanvasRepository orderCanvasRepository;
    private final CanvasRepository canvasRepository;
    private final OrderStateMachineService stateMachineService;

    private final Environment environment;

    /**
     * 模拟支付开关。真实环境部署时必须通过 PAYMENT_MOCK_ENABLED=false 关闭,
     * 否则任何人都可以自己把订单刷成"已支付"。
     */
    @Value("${payment.mock-enabled:true}")
    private boolean mockPaymentEnabled;

    @Value("${payment.callback.wechat-api-key:}")
    private String wechatCallbackKey;

    @Value("${payment.callback.alipay-api-key:}")
    private String alipayCallbackKey;

    /**
     * 创建定金支付单
     *
     * 前置条件:订单状态 = DEPOSIT_PENDING
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID
     * @param request 支付请求(含渠道选择)
     * @return 支付参数
     */
    @Transactional
    public PaymentResponse createDepositPayment(Long orderId, Long userId, PaymentCreateRequest request) {
        OrderEntity order = getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.DEPOSIT_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待付定金状态");
        }

        assertCanCreatePayment(orderId, PaymentType.DEPOSIT, "定金");

        // 创建支付单
        Payment payment = Payment.builder()
                .orderId(orderId)
                .type(PaymentType.DEPOSIT)
                .channel(request.getChannel())
                .amount(order.getDepositAmount())
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        log.info("创建定金支付单: paymentId={}, orderId={}, amount={}", payment.getId(), orderId, payment.getAmount());

        return buildPaymentResponse(payment, true);
    }

    /**
     * 创建尾款支付单
     *
     * 前置条件:订单状态 = BALANCE_PENDING
     */
    @Transactional
    public PaymentResponse createBalancePayment(Long orderId, Long userId, PaymentCreateRequest request) {
        OrderEntity order = getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.BALANCE_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待付尾款状态");
        }

        assertCanCreatePayment(orderId, PaymentType.BALANCE, "尾款");

        // 创建支付单
        Payment payment = Payment.builder()
                .orderId(orderId)
                .type(PaymentType.BALANCE)
                .channel(request.getChannel())
                .amount(order.getBalanceAmount())
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        log.info("创建尾款支付单: paymentId={}, orderId={}, amount={}", payment.getId(), orderId, payment.getAmount());

        return buildPaymentResponse(payment, true);
    }

    /**
     * 模拟支付回调(开发环境)
     *
     * 真实环境中此逻辑由微信/支付宝异步回调触发
     * 开发环境通过手动调用此接口模拟支付成功
     *
     * 安全约束:
     * 1. 仅在非 prod 环境且 payment.mock-enabled=true 时可用(环境隔离)
     * 2. 支付单所属订单必须属于调用者(归属校验),防止替他人刷支付
     *
     * @param paymentId 支付单 ID
     * @param userId 当前登录用户 ID
     */
    @Transactional
    public PaymentResponse simulatePaymentSuccess(Long paymentId, Long userId) {
        assertMockPaymentAllowed();

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "支付单不存在"));

        getOrderByIdAndUserId(payment.getOrderId(), userId);
        completePaidPayment(payment, "MOCK_" + System.currentTimeMillis(), payment.getAmount());
        log.info("模拟支付成功: paymentId={}, orderId={}, type={}", paymentId, payment.getOrderId(), payment.getType());
        return buildPaymentResponse(payment, true);
    }

    /**
     * 支付网关回调(非 mock)。验签由 Controller 完成后再进来。
     */
    @Transactional
    public void handleGatewayCallback(Long paymentId, String channelTxnId, BigDecimal paidAmount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "支付单不存在"));
        completePaidPayment(payment, channelTxnId, paidAmount);
    }

    public String wechatCallbackKey() {
        return wechatCallbackKey;
    }

    public String alipayCallbackKey() {
        return alipayCallbackKey;
    }

    /**
     * 取消时按应退金额记账。真实渠道退款需要商户号,这里只写 refunded_at / refund_amount。
     */
    @Transactional
    public void recordDepositRefund(Long orderId, BigDecimal refundAmount) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        paymentRepository.findByOrderIdAndType(orderId, PaymentType.DEPOSIT).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS && p.getRefundedAt() == null)
                .findFirst()
                .ifPresent(payment -> {
                    payment.setRefundAmount(refundAmount);
                    payment.setRefundedAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    log.info("已记录定金退款: paymentId={}, amount={}", payment.getId(), refundAmount);
                });
    }

    /**
     * 校验模拟支付是否可用
     *
     * 双重保护:显式开关 + prod 环境硬拒绝,避免部署时漏配环境变量导致资金口径被绕过。
     */
    private void assertMockPaymentAllowed() {
        if (!mockPaymentEnabled) {
            throw new BusinessException(ResultCode.FORBIDDEN, "模拟支付入口已关闭");
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "生产环境禁止使用模拟支付");
            }
        }
    }

    private void assertCanCreatePayment(Long orderId, PaymentType type, String label) {
        for (Payment existing : paymentRepository.findByOrderIdAndType(orderId, type)) {
            if (existing.getStatus() == PaymentStatus.SUCCESS) {
                throw new BusinessException(ResultCode.CONFLICT, "该订单" + label + "已支付成功");
            }
            if (existing.getStatus() == PaymentStatus.PENDING) {
                throw new BusinessException(ResultCode.CONFLICT, "已有待支付的" + label + "订单");
            }
        }
    }

    private void completePaidPayment(Payment payment, String channelTxnId, BigDecimal paidAmount) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("支付单已成功,幂等返回: paymentId={}", payment.getId());
            return;
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "支付单已失败,不可重复操作");
        }
        if (paidAmount == null || payment.getAmount().compareTo(paidAmount) != 0) {
            throw new BusinessException(ResultCode.PAYMENT_FAILED, "回调金额与支付单不一致");
        }

        OrderEntity order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));
        if (payment.getType() == PaymentType.DEPOSIT && order.getStatus() != OrderStatus.DEPOSIT_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待付定金状态,无法入账");
        }
        if (payment.getType() == PaymentType.BALANCE && order.getStatus() != OrderStatus.BALANCE_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待付尾款状态,无法入账");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setChannelTransactionId(channelTxnId);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        handlePaymentSuccess(payment);
    }

    /**
     * 支付成功后的订单状态联动(核心逻辑)
     *
     * 定金支付成功:
     * 1. 订单状态: DEPOSIT_PENDING → IN_PRODUCTION
     * 2. 所有关联画布锁定: locked = true
     *
     * 尾款支付成功:
     * 1. 订单状态: BALANCE_PENDING → SHIPPING_PENDING
     */
    private void handlePaymentSuccess(Payment payment) {
        OrderEntity order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));

        if (payment.getType() == PaymentType.DEPOSIT) {
            // 定金支付成功 → 生产中
            stateMachineService.transition(order, OrderStatus.IN_PRODUCTION,
                    OperatorType.SYSTEM, null, "定金支付成功");

            // 锁定所有关联画布
            List<OrderCanvas> orderCanvases = orderCanvasRepository.findByOrderId(order.getId());
            List<Long> canvasIds = orderCanvases.stream()
                    .map(OrderCanvas::getCanvasId)
                    .toList();
            List<Canvas> canvases = canvasRepository.findAllById(canvasIds);
            for (Canvas canvas : canvases) {
                canvas.setLocked(true);
            }
            canvasRepository.saveAll(canvases);

            log.info("定金支付成功,画布已锁定: orderId={}, canvasCount={}", order.getId(), canvases.size());

        } else if (payment.getType() == PaymentType.BALANCE) {
            // 尾款支付成功 → 待发货
            stateMachineService.transition(order, OrderStatus.SHIPPING_PENDING,
                    OperatorType.SYSTEM, null, "尾款支付成功");

            log.info("尾款支付成功: orderId={}", order.getId());
        }
    }

    /**
     * 查询订单的支付记录
     */
    public List<Payment> getPaymentHistory(Long orderId, Long userId) {
        // 校验订单归属权
        getOrderByIdAndUserId(orderId, userId);
        return paymentRepository.findByOrderId(orderId);
    }

    // ===== 辅助方法 =====

    private OrderEntity getOrderByIdAndUserId(Long orderId, Long userId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
        }
        return order;
    }

    private PaymentResponse buildPaymentResponse(Payment payment, boolean mockMode) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .type(payment.getType().name())
                .channel(payment.getChannel().name())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .payUrl(mockMode ? "/payment/mock?pid=" + payment.getId() : null)
                .mockMode(mockMode)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
