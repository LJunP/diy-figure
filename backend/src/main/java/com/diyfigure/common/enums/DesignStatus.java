package com.diyfigure.common.enums;

/**
 * 系列设计状态
 *
 * design_status 字段(series 表):
 * - DESIGNING: 系列创建,画布设计中
 * - READY_FOR_QUOTE: 已定稿画布数达到档位要求,可提交报价申请
 */
public enum DesignStatus {
    /** 设计中(画布数量未达标或用户仍在设计) */
    DESIGNING,
    /** 可提交报价(已定稿画布数达到档位要求的 designCount) */
    READY_FOR_QUOTE
}
