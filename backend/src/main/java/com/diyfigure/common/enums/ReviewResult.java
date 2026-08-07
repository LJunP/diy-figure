package com.diyfigure.common.enums;

/**
 * 终审结果
 *
 * review_log 表的 result 字段
 * 对应 02-产品需求说明书.md 第 3.4 节报价申请与人工终审
 *
 * ★ 终审是硬性阻塞点:
 * - APPROVED: 终审通过,进入人工报价环节
 * - REJECTED: 终审不通过,拒绝报价,用户可修改画布后重新提交
 * 不可自动通过,不可跳过
 */
public enum ReviewResult {
    /** 终审通过 */
    APPROVED,
    /** 终审拒绝(注明原因) */
    REJECTED
}
