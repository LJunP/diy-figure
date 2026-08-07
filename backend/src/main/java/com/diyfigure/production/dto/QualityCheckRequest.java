package com.diyfigure.production.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 质检请求 DTO
 */
@Data
public class QualityCheckRequest {

    @NotNull(message = "质检结果不能为空")
    private String result; // PASSED / FAILED

    /** 质检不通过原因(FAILED 时必填) */
    private String failReason;
}
