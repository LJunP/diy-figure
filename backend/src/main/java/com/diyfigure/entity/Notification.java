package com.diyfigure.entity;

import com.diyfigure.common.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 通知表(对应 03-技术设计说明书.md 第 5.12 节)
 *
 * 通知渠道:
 * - IN_APP(站内消息):全量覆盖所有状态变化
 * - EMAIL(邮件):仅在报价确认、需付尾款、已发货三个关键节点触发
 */
@Entity
@Table(name = "notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接收用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 关联订单 ID(可为 null,如系统公告) */
    @Column(name = "order_id")
    private Long orderId;

    /** 通知内容 */
    @Column(nullable = false, length = 500)
    private String content;

    /** 通知渠道:IN_APP / EMAIL */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    /** 已读时间(null = 未读) */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
