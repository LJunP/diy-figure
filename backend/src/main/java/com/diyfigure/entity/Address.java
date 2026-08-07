package com.diyfigure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 收货地址表(对应 03-技术设计说明书.md 第 5.7 节)
 *
 * 抽奖完成后、支付定金前强制填写
 * 一个用户可有多个地址,订单支付时选择一个关联
 */
@Entity
@Table(name = "address")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 收货人姓名 */
    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    /** 收货人电话 */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /** 详细收货地址 */
    @Column(name = "detail", nullable = false, length = 500)
    private String detail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
