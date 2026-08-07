package com.diyfigure.common.enums;

/**
 * 支付类型
 *
 * payment 表的 type 字段:
 * - DEPOSIT: 定金(50%,接受报价+抽奖后支付)
 * - BALANCE: 尾款(50%,质检通过后支付)
 */
public enum PaymentType {
    /** 定金支付 */
    DEPOSIT,
    /** 尾款支付 */
    BALANCE
}
