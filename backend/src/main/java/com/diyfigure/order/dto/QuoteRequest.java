package com.diyfigure.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运营端报价请求 DTO
 *
 * 终审通过后,运营填写报价和预计交期
 */
@Data
public class QuoteRequest {

    @NotNull(message = "报价金额不能为空")
    private BigDecimal quotedPrice;

    /** 预计交货日期(可选) */
    private String expectedDeliveryDate;
}
