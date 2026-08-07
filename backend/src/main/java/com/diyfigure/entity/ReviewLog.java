package com.diyfigure.entity;

import com.diyfigure.common.enums.ReviewResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 人工终审记录表(对应 03-技术设计说明书.md 第 5.8 节)
 *
 * ★ 终审是硬性阻塞点:
 * - 用户提交报价申请后,订单进入 REVIEWING(终审中)状态
 * - 必须由管理员手动调用审核接口,不可自动跳过
 * - APPROVED → 进入报价环节;REJECTED → 注明原因,用户可修改后重新提交
 */
@Entity
@Table(name = "review_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联订单 ID */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 审核人 ID(运营账号) */
    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    /** 审核结果:APPROVED / REJECTED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReviewResult result;

    /** 拒绝原因(REJECTED 时填写) */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** 审核时间 */
    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;
}
