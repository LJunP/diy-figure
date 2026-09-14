package com.diyfigure.order.service;

import com.diyfigure.common.enums.OperatorType;
import com.diyfigure.common.enums.OrderStatus;
import com.diyfigure.common.exception.IllegalStateTransitionException;
import com.diyfigure.entity.OrderEntity;
import com.diyfigure.entity.OrderStatusLog;
import com.diyfigure.repository.OrderStatusLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单状态机服务
 *
 * 核心职责:
 * 1. 状态转移校验:所有状态转移必须经过 canTransitionTo() 校验
 * 2. 状态变更审计日志:每次状态转移必须写入 order_status_log 表(03 文档 5.11 节)
 * 3. 操作者记录:记录触发状态转移的操作者类型(USER/ADMIN/SYSTEM)和 ID
 *
 * ★ 这是状态机可追溯性的核心,任何状态转移都必须调用此服务的 transition() 方法
 * 不允许直接 UPDATE order.status 而不落审计日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStateMachineService {

    private final OrderStatusLogRepository orderStatusLogRepository;

    /**
     * 通知是旁路能力:用 ObjectProvider 注入,且调用失败只记日志,
     * 保证"通知出问题"永远不会阻断订单状态流转。
     */
    private final org.springframework.beans.factory.ObjectProvider<com.diyfigure.notification.NotificationService> notificationServiceProvider;

    /**
     * 状态转移
     *
     * @param order 订单对象
     * @param toStatus 目标状态
     * @param operatorType 操作者类型(USER/ADMIN/SYSTEM)
     * @param operatorId 操作者 ID(SYSTEM 时可为 null)
     * @param reason 状态变更原因(可选)
     */
    @Transactional
    public void transition(OrderEntity order, OrderStatus toStatus,
                            OperatorType operatorType, Long operatorId, String reason) {
        OrderStatus fromStatus = order.getStatus();

        // 1. 校验状态转移是否合法
        if (!fromStatus.canTransitionTo(toStatus)) {
            throw new IllegalStateTransitionException(fromStatus.name(), toStatus.name());
        }

        // 2. 更新订单状态
        order.setStatus(toStatus);

        // 3. 写入审计日志
        OrderStatusLog statusLog = OrderStatusLog.builder()
                .orderId(order.getId())
                .fromStatus(fromStatus.name())
                .toStatus(toStatus.name())
                .operatorType(operatorType)
                .operatorId(operatorId)
                .reason(reason)
                .build();
        orderStatusLogRepository.save(statusLog);

        // 4. 产生通知(站内消息 + 三个关键节点的邮件)。失败不影响状态流转
        try {
            notificationServiceProvider.ifAvailable(svc ->
                    svc.notifyOrderStatusChanged(order.getId(), order.getUserId(), toStatus));
        } catch (Exception e) {
            log.error("订单通知发送失败(已忽略): orderId={}, toStatus={}, error={}",
                    order.getId(), toStatus, e.getMessage());
        }

        log.info("订单状态转移: orderId={}, {} → {}, operator={}, reason={}",
                order.getId(), fromStatus, toStatus, operatorType, reason);
    }

    /**
     * 批量状态转移(用于定时任务等场景)
     *
     * @param orders 订单列表
     * @param toStatus 目标状态
     * @param operatorType 操作者类型
     * @param operatorId 操作者 ID
     * @param reason 状态变更原因
     */
    @Transactional
    public void transitionBatch(java.util.List<OrderEntity> orders, OrderStatus toStatus,
                                OperatorType operatorType, Long operatorId, String reason) {
        for (OrderEntity order : orders) {
            try {
                transition(order, toStatus, operatorType, operatorId, reason);
            } catch (Exception e) {
                log.error("批量状态转移失败: orderId={}, toStatus={}", order.getId(), toStatus, e);
            }
        }
    }
}
