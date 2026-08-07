package com.diyfigure.entity;

import com.diyfigure.common.enums.QcResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 质检记录表(对应 03-技术设计说明书.md 第 5.9 节)
 *
 * 厂家生产完成后货物先回平台质检:
 * - PASSED: 质检通过,通知用户付尾款
 * - FAILED: 质检不通过,返厂返工(订单回到 IN_PRODUCTION 状态)
 */
@Entity
@Table(name = "quality_check_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityCheckLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联订单 ID */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 质检结果:PASSED / FAILED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QcResult result;

    /** 质检不通过原因(FAILED 时填写) */
    @Column(name = "fail_reason", length = 500)
    private String failReason;

    /** 质检时间 */
    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;
}
