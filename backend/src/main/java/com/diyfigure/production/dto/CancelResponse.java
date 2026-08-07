package com.diyfigure.production.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 取消订单响应 DTO
 *
 * 包含违约金计算结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelResponse {

    private Long orderId;
    private String stageAtCancel;       // 取消时所处的状态
    private BigDecimal depositRefundAmount; // 退还金额
    private BigDecimal depositPenaltyAmount; // 扣除金额(违约金)
    private String description;          // 取消说明(违约金计算依据)
}
