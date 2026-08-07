package com.diyfigure.order.dto;

import com.diyfigure.common.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {

    private Long id;
    private Long userId;
    private String orderType;
    private Long parentOrderId;
    private String status;
    private BigDecimal quotedPrice;
    private BigDecimal depositAmount;
    private BigDecimal balanceAmount;
    private LocalDate expectedDeliveryDate;
    private String trackingNumber;
    private String trackingCompany;
    private LocalDateTime createdAt;

    /** 关联系列信息 */
    private SeriesInfo series;

    /** 关联画布信息(主订单:抽奖后展示中签/未中签;补购:单画布) */
    private List<CanvasInfo> canvases;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeriesInfo {
        private Long id;
        private String name;
        private String specTier;
        private String sizeTier;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CanvasInfo {
        private Long canvasId;
        private String name;
        private String status;
        private String firstConceptImage;
        private String lotteryResult; // SELECTED / NOT_SELECTED
        private String refillAvailableUntil;
    }
}
