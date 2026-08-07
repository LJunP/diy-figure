package com.diyfigure.common.enums;

/**
 * 订单类型
 * 主订单与补购订单共用 order 表,通过此字段区分
 */
public enum OrderType {
    /** 主订单(正常定制流程) */
    MAIN,
    /** 补购订单(未中签角色在 60 天窗口内补购,复用已定稿设计) */
    REFILL
}
