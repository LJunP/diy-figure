package com.diyfigure.common.enums;

/**
 * 支付状态
 *
 * payment 表的 status 字段
 */
public enum PaymentStatus {
    /** 待支付(已创建支付单,等待用户完成支付) */
    PENDING,
    /** 支付成功(收到支付网关回调确认) */
    SUCCESS,
    /** 支付失败 */
    FAILED
}
