package com.diyfigure.common.enums;

/**
 * 抽奖结果
 *
 * order_canvas 表的 lottery_result 字段:
 * - SELECTED: 中签(该角色将被实际生产)
 * - NOT_SELECTED: 未中签(该角色在 60 天窗口内可补购)
 */
public enum LotteryResult {
    /** 中签(实际生产) */
    SELECTED,
    /** 未中签(可补购) */
    NOT_SELECTED
}
