package com.diyfigure.common.enums;

/**
 * 操作者类型
 *
 * order_status_log 表的 operator_type 字段
 * 记录每次状态变更是由谁触发的,用于审计追溯
 */
public enum OperatorType {
    /** 用户操作(前端用户触发) */
    USER,
    /** 管理员操作(运营人员触发) */
    ADMIN,
    /** 系统自动(定时任务、回调等) */
    SYSTEM
}
