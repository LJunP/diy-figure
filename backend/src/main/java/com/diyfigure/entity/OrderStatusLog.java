package com.diyfigure.entity;

import com.diyfigure.common.enums.OperatorType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 状态变更审计日志表(对应 03-技术设计说明书.md 第 5.11 节)
 *
 * ★ 这是状态机可追溯性的核心——任何状态转移都必须写入一条记录,
 * 不允许直接 UPDATE order.status 而不落审计日志。
 * 这是 service 层的强制约束,不是可选项。
 */
@Entity
@Table(name = "order_status_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联订单 ID */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 变更前状态 */
    @Column(name = "from_status", length = 30)
    private String fromStatus;

    /** 变更后状态 */
    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    /** 操作者类型:USER / ADMIN / SYSTEM */
    @Enumerated(EnumType.STRING)
    @Column(name = "operator_type", nullable = false, length = 10)
    private OperatorType operatorType;

    /** 操作者 ID(SYSTEM 时为 null) */
    @Column(name = "operator_id")
    private Long operatorId;

    /** 变更原因(可选) */
    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
