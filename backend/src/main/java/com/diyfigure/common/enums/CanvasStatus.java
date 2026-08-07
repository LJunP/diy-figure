package com.diyfigure.common.enums;

/**
 * 画布状态
 *
 * 对应 02-产品需求说明书.md 第 4.1 节:
 * 设计中 → 已定稿
 * (已定稿画布在设计锁定前,用户仍可重新打开修改,修改后需重新标记"已定稿")
 */
public enum CanvasStatus {
    /** 设计中(用户正在与 AI 协作设计) */
    DESIGNING,
    /** 已定稿(用户确认设计完成,等待提交报价审核) */
    FINALIZED
}
