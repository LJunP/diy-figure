package com.diyfigure.entity;

import com.diyfigure.common.enums.LotteryResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单-画布关联表(对应 03-技术设计说明书.md 第 5.5 节)
 *
 * 记录抽奖结果:每个参与抽奖的画布一条记录
 * - SELECTED: 中签(实际生产)
 * - NOT_SELECTED: 未中签(refill_available_until = 主订单发货日 + 60天,窗口内可补购)
 */
@Entity
@Table(name = "order_canvas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCanvas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属订单 ID */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 关联画布 ID */
    @Column(name = "canvas_id", nullable = false)
    private Long canvasId;

    /** 抽奖结果:SELECTED / NOT_SELECTED */
    @Enumerated(EnumType.STRING)
    @Column(name = "lottery_result", nullable = false, length = 20)
    private LotteryResult lotteryResult;

    /**
     * 补购截止日期
     * SELECTED 角色:不设值(null)
     * NOT_SELECTED 角色:主订单发货日 + 60天
     * 过期后该角色不可补购
     */
    @Column(name = "refill_available_until")
    private LocalDate refillAvailableUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
