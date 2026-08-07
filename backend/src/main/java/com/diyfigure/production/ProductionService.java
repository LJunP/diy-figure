package com.diyfigure.production;

import com.diyfigure.common.enums.*;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.*;
import com.diyfigure.order.service.OrderStateMachineService;
import com.diyfigure.production.dto.*;
import com.diyfigure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产与物流服务
 *
 * 核心职责:
 * 1. 生产开工标记:运营手动标记 production_started_at(用于违约金判定)
 * 2. 质检:IN_PRODUCTION → QC_PENDING → PASSED(BALANCE_PENDING) / FAILED(回 IN_PRODUCTION)
 * 3. 发货:SHIPPING_PENDING → SHIPPED(录入物流单号,设置未中签角色补购窗口)
 * 4. 订单完成:SHIPPED → COMPLETED(用户签收或运营标记)
 * 5. 取消与违约金:根据 status + production_started_at 计算违约金
 *
 * ★ 违约金三档规则(对应 02 文档 4.4 节):
 * - 未付定金前取消:无损失,全额退还(如已付定金,退还定金100%)
 * - 已付定金未开工取消:扣除定金 30% 作服务费,退还 70%
 * - 已开工取消:定金不退
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionService {

    private final OrderRepository orderRepository;
    private final OrderCanvasRepository orderCanvasRepository;
    private final QualityCheckLogRepository qcLogRepository;
    private final CancellationLogRepository cancellationLogRepository;
    private final OrderStateMachineService stateMachineService;

    // ===== 运营端接口 =====

    /**
     * 运营端: 标记生产实际开工
     *
     * 记录 production_started_at,用于违约金判定
     * 此操作不改变订单状态(订单仍为 IN_PRODUCTION),只设置内部字段
     *
     * @param orderId 订单 ID
     * @param adminId 运营 ID
     */
    @Transactional
    public void markProductionStarted(Long orderId, Long adminId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.IN_PRODUCTION) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在生产中状态");
        }

        if (order.getProductionStartedAt() != null) {
            throw new BusinessException(ResultCode.CONFLICT, "已标记过生产开工时间");
        }

        order.setProductionStartedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("标记生产开工: orderId={}, adminId={}, startedAt={}",
                orderId, adminId, order.getProductionStartedAt());
    }

    /**
     * 运营端: 质检
     *
     * 前置条件:订单状态 = QC_PENDING
     * - PASSED: QC_PENDING → BALANCE_PENDING(通知用户付尾款)
     * - FAILED: QC_PENDING → IN_PRODUCTION(返厂返工)
     *
     * @param orderId 订单 ID
     * @param adminId 运营 ID
     * @param request 质检请求
     */
    @Transactional
    public void qualityCheck(Long orderId, Long adminId, QualityCheckRequest request) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.QC_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待质检状态");
        }

        QcResult result = QcResult.valueOf(request.getResult());

        // 记录质检日志
        QualityCheckLog qcLog = QualityCheckLog.builder()
                .orderId(orderId)
                .result(result)
                .failReason(result == QcResult.FAILED ? request.getFailReason() : null)
                .checkedAt(LocalDateTime.now())
                .build();
        qcLogRepository.save(qcLog);

        // 状态转移
        if (result == QcResult.PASSED) {
            stateMachineService.transition(order, OrderStatus.BALANCE_PENDING,
                    OperatorType.ADMIN, adminId, "质检通过");
        } else {
            stateMachineService.transition(order, OrderStatus.IN_PRODUCTION,
                    OperatorType.ADMIN, adminId, "质检不通过: " + request.getFailReason());
        }

        log.info("质检完成: orderId={}, result={}", orderId, result);
    }

    /**
     * 运营端: 发货
     *
     * 前置条件:订单状态 = SHIPPING_PENDING
     * - 录入物流公司 + 物流单号
     * - 状态转移: SHIPPING_PENDING → SHIPPED
     * - 设置未中签角色的补购窗口(refill_available_until = 发货日 + 60天)
     *
     * @param orderId 订单 ID
     * @param adminId 运营 ID
     * @param request 发货请求
     */
    @Transactional
    public void shipOrder(Long orderId, Long adminId, ShipRequest request) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.SHIPPING_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待发货状态");
        }

        // 录入物流信息
        order.setTrackingCompany(request.getTrackingCompany());
        order.setTrackingNumber(request.getTrackingNumber());

        // 状态转移: SHIPPING_PENDING → SHIPPED
        stateMachineService.transition(order, OrderStatus.SHIPPED,
                OperatorType.ADMIN, adminId, "发货: " + request.getTrackingCompany() + " " + request.getTrackingNumber());

        // 设置未中签角色的补购窗口 = 发货日 + 60天
        List<OrderCanvas> orderCanvases = orderCanvasRepository.findByOrderId(orderId);
        java.time.LocalDate refillDeadline = java.time.LocalDate.now().plusDays(60);
        for (OrderCanvas oc : orderCanvases) {
            if (oc.getLotteryResult() == LotteryResult.NOT_SELECTED) {
                oc.setRefillAvailableUntil(refillDeadline);
            }
        }
        orderCanvasRepository.saveAll(orderCanvases);

        log.info("发货完成: orderId={}, tracking={} {}, refillDeadline={}",
                orderId, request.getTrackingCompany(), request.getTrackingNumber(), refillDeadline);
    }

    /**
     * 运营端: 标记订单完成
     *
     * 前置条件:订单状态 = SHIPPED
     * SHIPPED → COMPLETED
     */
    @Transactional
    public void completeOrder(Long orderId, Long adminId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在已发货状态");
        }

        stateMachineService.transition(order, OrderStatus.COMPLETED,
                OperatorType.ADMIN, adminId, "运营标记完成");

        log.info("订单完成: orderId={}, adminId={}", orderId, adminId);
    }

    // ===== 用户端接口 =====

    /**
     * 用户端: 确认签收
     *
     * 前置条件:订单状态 = SHIPPED
     * SHIPPED → COMPLETED
     */
    @Transactional
    public void confirmReceived(Long orderId, Long userId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在已发货状态");
        }

        stateMachineService.transition(order, OrderStatus.COMPLETED,
                OperatorType.USER, userId, "用户确认签收");

        log.info("用户确认签收: orderId={}, userId={}", orderId, userId);
    }

    /**
     * 用户端: 取消订单
     *
     * ★ 违约金三档规则(02 文档 4.4 节):
     * 1. 未付定金前取消(stage < DEPOSIT_PENDING):无损失
     * 2. 已付定金未开工取消(production_started_at == null):
     *    扣除定金 30% 作服务费,退还 70%
     * 3. 已开工取消(production_started_at != null):
     *    定金不退
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID
     * @return 取消结果(含违约金计算)
     */
    @Transactional
    public CancelResponse cancelOrder(Long orderId, Long userId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
        }

        // 检查是否可以取消
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前状态不可取消");
        }

        String stageAtCancel = order.getStatus().name();
        BigDecimal refundAmount = BigDecimal.ZERO;
        BigDecimal penaltyAmount = BigDecimal.ZERO;
        String description;

        // 判断违约金档位
        if (order.getStatus().ordinal() < OrderStatus.DEPOSIT_PENDING.ordinal()) {
            // 档位1:未付定金前取消,无损失
            description = "未付定金前取消,无损失";
        } else if (order.getProductionStartedAt() == null) {
            // 档位2:已付定金未开工,扣除定金30%,退还70%
            if (order.getDepositAmount() != null) {
                penaltyAmount = order.getDepositAmount()
                        .multiply(new BigDecimal("0.30"))
                        .setScale(2, RoundingMode.HALF_UP);
                refundAmount = order.getDepositAmount()
                        .subtract(penaltyAmount)
                        .setScale(2, RoundingMode.HALF_UP);
            }
            description = "已付定金未开工取消,扣除定金30%作服务费,退还70%";
        } else {
            // 档位3:已开工,定金不退
            if (order.getDepositAmount() != null) {
                penaltyAmount = order.getDepositAmount();
            }
            description = "已开工取消,定金不退";
        }

        // 记录取消日志
        CancellationLog cancelLog = CancellationLog.builder()
                .orderId(orderId)
                .stageAtCancel(stageAtCancel)
                .depositRefundAmount(refundAmount)
                .depositPenaltyAmount(penaltyAmount)
                .cancelledAt(LocalDateTime.now())
                .build();
        cancellationLogRepository.save(cancelLog);

        // 状态转移: → CANCELLED
        stateMachineService.transition(order, OrderStatus.CANCELLED,
                OperatorType.USER, userId, description);

        log.info("订单取消: orderId={}, stage={}, refund={}, penalty={}",
                orderId, stageAtCancel, refundAmount, penaltyAmount);

        return CancelResponse.builder()
                .orderId(orderId)
                .stageAtCancel(stageAtCancel)
                .depositRefundAmount(refundAmount)
                .depositPenaltyAmount(penaltyAmount)
                .description(description)
                .build();
    }

    /**
     * 运营端: 查看所有取消记录
     */
    public List<CancellationLog> listCancellations() {
        return cancellationLogRepository.findAllByOrderByCancelledAtDesc();
    }

    /**
     * 查看订单的质检记录
     */
    public List<QualityCheckLog> getQcLogs(Long orderId) {
        return qcLogRepository.findByOrderIdOrderByCheckedAtDesc(orderId);
    }
}
