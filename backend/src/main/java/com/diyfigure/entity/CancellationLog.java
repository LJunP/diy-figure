package com.diyfigure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 取消/违约记录表(对应 03-技术设计说明书.md 第 5.10 节)
 *
 * 记录取消订单时的违约金计算依据:
 * - 未付定金前取消:无损失(stage_at_cancel 记录当时状态)
 * - 已付定金未开工取消:扣除部分定金(参考30%)作服务费,剩余退还
 * - 已开工取消:定金不退
 *
 * 违约金档位判定依据:status + production_started_at 是否为空
 */
@Entity
@Table(name = "cancellation_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联订单 ID */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 取消时所处状态(用于追溯违约金计算依据) */
    @Column(name = "stage_at_cancel", nullable = false, length = 30)
    private String stageAtCancel;

    /** 退还金额 */
    @Column(name = "deposit_refund_amount", precision = 10, scale = 2)
    private BigDecimal depositRefundAmount;

    /** 扣除金额(服务费/违约金) */
    @Column(name = "deposit_penalty_amount", precision = 10, scale = 2)
    private BigDecimal depositPenaltyAmount;

    /** 取消时间 */
    @Column(name = "cancelled_at", nullable = false)
    private LocalDateTime cancelledAt;
}
