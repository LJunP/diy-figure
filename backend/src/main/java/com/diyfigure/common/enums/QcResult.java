package com.diyfigure.common.enums;

/**
 * 质检结果
 *
 * quality_check_log 表的 result 字段
 *
 * - PASSED: 质检通过,可通知用户付尾款
 * - FAILED: 质检不通过,返厂返工(回到"生产中"状态)
 */
public enum QcResult {
    /** 质检通过 */
    PASSED,
    /** 质检不通过(返厂返工) */
    FAILED
}
