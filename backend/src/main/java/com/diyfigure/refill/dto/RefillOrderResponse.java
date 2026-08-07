package com.diyfigure.refill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 补购订单响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefillOrderResponse {

    private Long refillOrderId;
    private Long parentOrderId;
    private Long canvasId;
    private String canvasName;
    private BigDecimal refillPrice;
    private BigDecimal depositAmount;
    private BigDecimal balanceAmount;
    private String status;
    private String paymentUrl;
}
