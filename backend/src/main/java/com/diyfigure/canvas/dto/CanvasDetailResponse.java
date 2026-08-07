package com.diyfigure.canvas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 画布详情响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanvasDetailResponse {

    private Long id;
    private Long seriesId;
    private String status;
    private List<String> conceptImageUrls;
    private String model3dUrl;
    private List<Map<String, Object>> aiConversation;
    private Boolean locked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finalizedAt;
}
