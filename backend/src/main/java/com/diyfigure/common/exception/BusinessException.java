package com.diyfigure.common.exception;

import com.diyfigure.common.response.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 * 在 service 层抛出,由 GlobalExceptionHandler 统一捕获并返回给前端
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
