package com.diyfigure.common.enums;

/**
 * 支付渠道
 *
 * payment 表的 channel 字段
 * V1 支持微信支付 + 支付宝,Stripe 留待 V2
 */
public enum PaymentChannel {
    /** 微信支付 */
    WECHAT,
    /** 支付宝 */
    ALIPAY
}
