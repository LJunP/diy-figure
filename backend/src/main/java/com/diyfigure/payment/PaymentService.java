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

        // 检查是否已有 PENDING 的定金支付单
        paymentRepository.findByOrderIdAndType(orderId, PaymentType.DEPOSIT)
                .ifPresent(existing -> {
                    if (existing.getStatus() == PaymentStatus.PENDING) {
                        throw new BusinessException(ResultCode.CONFLICT, "已有待支付的定金订单");
                    }
                });

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

        // 检查是否已有 PENDING 的尾款支付单
        paymentRepository.findByOrderIdAndType(orderId, PaymentType.BALANCE)
                .ifPresent(existing -> {
                    if (existing.getStatus() == PaymentStatus.PENDING) {
                        throw new BusinessException(ResultCode.CONFLICT, "已有待支付的尾款订单");
                    }
                });

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
     * @param paymentId 支付单 ID
     */
    @Transactional
    public PaymentResponse simulatePaymentSuccess(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "支付单不存在"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            // 幂等:已支付成功,直接返回
            log.info("支付单已成功,幂等返回: paymentId={}", paymentId);
            return buildPaymentResponse(payment, true);
        }

        if (payment.getStatus() == PaymentStatus.FAILED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "支付单已失败,不可重复操作");
        }

        // 更新支付状态
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setChannelTransactionId("MOCK_" + System.currentTimeMillis());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 联动订单状态
        handlePaymentSuccess(payment);

        log.info("模拟支付成功: paymentId={}, orderId={}, type={}", paymentId, payment.getOrderId(), payment.getType());

        return buildPaymentResponse(payment, true);
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
