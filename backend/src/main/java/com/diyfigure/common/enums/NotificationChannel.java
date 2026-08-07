package com.diyfigure.common.enums;

/**
 * 通知渠道
 *
 * notification 表的 channel 字段
 * 对应 02-产品需求说明书.md 第 5 节:
 * - 站内消息: 全量覆盖所有状态变化
 * - 邮件通知: 仅在报价确认、需付尾款、已发货三个关键节点触发
 */
public enum NotificationChannel {
    /** 站内消息(全量覆盖) */
    IN_APP,
    /** 邮件(仅关键节点:报价确认/需付尾款/已发货) */
    EMAIL
}
