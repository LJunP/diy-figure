package com.diyfigure.series.dto;

import com.diyfigure.common.enums.CanvasStatus;
import com.diyfigure.common.enums.DesignStatus;
import com.diyfigure.common.enums.SpecTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系列详情响应 DTO(含画布列表)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesDetailResponse {

    private Long id;
    private Long userId;
    private String name;
    private SpecTier specTier;
    private String sizeTier;
    private DesignStatus designStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 画布列表 */
    private List<CanvasSummary> canvases;

    /** 已定稿画布数量 */
    private Long finalizedCount;

    /** 总画布数量 */
    private Long totalCanvasCount;

    /**
     * 画布摘要信息(列表展示用)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CanvasSummary {
        private Long id;
        private CanvasStatus status;
        private String model3dUrl;
        private String firstConceptImage;
        private Boolean locked;
        private LocalDateTime createdAt;
        private LocalDateTime finalizedAt;
    }
}
