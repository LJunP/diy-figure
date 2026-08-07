package com.diyfigure.entity;

import com.diyfigure.common.enums.PaymentChannel;
import com.diyfigure.common.enums.PaymentStatus;
import com.diyfigure.common.enums.PaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水表(对应 03-技术设计说明书.md 第 5.6 节)
 *
 * 每次创建支付单(定金/尾款)生成一条记录
 * 支付网关回调确认后更新 status 和 paid_at
 */
@Entity
@Table(name = "payment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属订单 ID */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 支付类型:DEPOSIT(定金) / BALANCE(尾款) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentType type;

    /** 支付渠道:WECHAT / ALIPAY */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentChannel channel;

    /** 支付金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** 支付状态:PENDING / SUCCESS / FAILED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentStatus status;

    /** 支付网关返回的交易号,用于核对回调 */
    @Column(name = "channel_transaction_id", length = 100)
    private String channelTransactionId;

    /** 支付成功时间(回调确认后写入) */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
