package com.diyfigure.common.enums;

/**
 * 用户角色
 * V1 运营账号也存在 user 表,用角色区分,不单独建管理员表
 */
public enum UserRole {
    /** 普通用户 */
    USER,
    /** 平台运营(管理端) */
    ADMIN
}
