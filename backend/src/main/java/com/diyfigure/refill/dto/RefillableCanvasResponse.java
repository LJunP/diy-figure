package com.diyfigure.refill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 可补购角色信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefillableCanvasResponse {

    private Long orderCanvasId;
    private Long canvasId;
    private String canvasName;
    private String firstConceptImage;
    private String lotteryResult;       // NOT_SELECTED
    private LocalDate refillAvailableUntil;
    private Boolean expired;            // 补购窗口是否已过期
    private Boolean alreadyRefilled;
    private BigDecimal refillPrice;     // 自动计算的补购价格
}
