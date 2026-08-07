package com.diyfigure.entity;

import com.diyfigure.common.enums.OrderStatus;
import com.diyfigure.common.enums.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单表 - 交易状态机(对应 03-技术设计说明书.md 第 5.4 节)
 *
 * 核心设计:主订单与补购订单共用此表,通过 order_type 区分(MAIN / REFILL)
 * 补购订单通过 parent_order_id 指向对应的主订单
 *
 * ★ 关键字段说明:
 * - status: 14 个主订单状态 + CANCELLED,转换规则见 OrderStatus 枚举
 * - production_started_at: 运营手动标记的"生产实际开工时间",用于违约金判定
 *   (不对用户可见,内部区分"生产中"状态下的罚金等级)
 *   注意:此字段名遵循 03 文档定义,与 handoff prompt 中的 actual_start_time 概念相同
 *
 * ★ 注意:表名 "order" 是 MySQL 保留字,使用反引号转义
 */
@Entity
@Table(name = "`order`")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属系列 ID */
    @Column(name = "series_id", nullable = false)
    private Long seriesId;

    /** 下单用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 订单类型:MAIN(主订单) / REFILL(补购订单) */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    /** 父订单 ID(REFILL 类型时指向对应的 MAIN 订单) */
    @Column(name = "parent_order_id")
    private Long parentOrderId;

    /** 订单状态(见 OrderStatus 枚举,14 个主订单状态 + CANCELLED) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    /** 报价金额(MAIN:运营填写 / REFILL:自动计算 = 套餐总价÷中签数×1.3) */
    @Column(name = "quoted_price", precision = 10, scale = 2)
    private BigDecimal quotedPrice;

    /** 预计交货日期(报价时一并给出) */
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    /** 定金金额(50%) */
    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;

    /** 尾款金额(50%) */
    @Column(name = "balance_amount", precision = 10, scale = 2)
    private BigDecimal balanceAmount;

    /**
     * 生产实际开工时间(运营手动标记)
     * 用于违约金判定:区分"已付定金但未开工"和"已开工"两档罚金
     * 不对用户可见,不是独立状态
     */
    @Column(name = "production_started_at")
    private LocalDateTime productionStartedAt;

    /** 收货地址 ID(抽奖完成后填写) */
    @Column(name = "address_id")
    private Long addressId;

    /** 物流单号(发货时录入) */
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    /** 物流公司(发货时录入) */
    @Column(name = "tracking_company", length = 50)
    private String trackingCompany;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
