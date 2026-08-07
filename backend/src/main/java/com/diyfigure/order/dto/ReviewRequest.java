package com.diyfigure.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 运营端审核请求 DTO
 *
 * 管理员审核设计合规性,决定是否通过
 */
@Data
public class ReviewRequest {

    @NotNull(message = "审核结果不能为空")
    private String result; // APPROVED / REJECTED

    /** 拒绝原因(REJECTED 时必填) */
    private String rejectReason;
}
