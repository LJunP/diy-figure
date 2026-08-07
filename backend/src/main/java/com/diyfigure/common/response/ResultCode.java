package com.diyfigure.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务状态码枚举
 * 2xx: 成功
 * 4xx: 客户端错误
 * 5xx: 服务端错误
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ===== 成功 =====
    SUCCESS(200, "操作成功"),

    // ===== 通用错误 4xx =====
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),

    // ===== 业务错误 4xx =====
    USER_ALREADY_EXISTS(4001, "用户已存在"),
    USER_NOT_FOUND(4002, "用户不存在"),
    PASSWORD_INCORRECT(4003, "密码错误"),
    ACCOUNT_DISABLED(4004, "账号已禁用"),

    SERIES_NOT_FOUND(4101, "系列不存在"),
    CANVAS_NOT_FOUND(4102, "画布不存在"),
    CANVAS_NOT_FINALIZED(4103, "画布未定稿"),
    CANVAS_LOCKED(4104, "画布已锁定,不可修改"),
    FINALIZED_COUNT_NOT_ENOUGH(4105, "已定稿画布数量未达到档位要求"),

    ORDER_NOT_FOUND(4201, "订单不存在"),
    ILLEGAL_STATE_TRANSITION(4202, "非法的状态机转换"),
    REVIEW_NOT_APPROVED(4203, "终审未通过,不可报价"),
    QUOTE_ALREADY_FILLED(4204, "报价已填写"),
    LOTTERY_ALREADY_DONE(4205, "已抽奖,不可重复抽奖"),
    ADDRESS_REQUIRED(4206, "请先填写收货地址"),
    PAYMENT_FAILED(4207, "支付失败"),
    REFILL_WINDOW_EXPIRED(4208, "补购窗口已过期"),
    REFILL_NOT_AVAILABLE(4209, "该角色不可补购"),

    // ===== 文件上传错误 =====
    FILE_UPLOAD_FAILED(4301, "文件上传失败"),
    FILE_TYPE_NOT_SUPPORTED(4302, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(4303, "文件大小超出限制"),

    // ===== AI 服务错误 =====
    AI_SERVICE_ERROR(4401, "AI 服务调用失败"),
    AI_CONTENT_VIOLATION(4402, "AI 内容审核未通过:涉及侵权或不当内容"),

    // ===== 服务端错误 5xx =====
    INTERNAL_ERROR(5000, "系统内部错误"),
    DATABASE_ERROR(5001, "数据库操作失败"),
    EXTERNAL_SERVICE_ERROR(5002, "外部服务调用失败");

    private final int code;
    private final String message;
}
