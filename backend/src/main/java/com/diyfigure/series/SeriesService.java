package com.diyfigure.series;

import com.diyfigure.common.enums.DesignStatus;
import com.diyfigure.common.enums.SpecTier;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.Canvas;
import com.diyfigure.entity.Series;
import com.diyfigure.repository.CanvasRepository;
import com.diyfigure.repository.SeriesRepository;
import com.diyfigure.series.dto.SeriesCreateRequest;
import com.diyfigure.series.dto.SeriesDetailResponse;
import com.diyfigure.series.dto.SeriesUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系列服务
 *
 * 系列是设计容器,包含多个画布(每个画布 = 一个原创角色设计)
 * 系列只负责设计阶段的数据,交易状态在 order 表中管理(03 文档第 4 节)
 *
 * 规格档位决定需要的画布数量:
 * - LIGHT: 6 选 4
 * - CLASSIC: 9 选 6
 * - COLLECTION: 12 选 8
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final CanvasRepository canvasRepository;

    /**
     * 创建系列
     *
     * 新建系列初始状态为 DESIGNING
     */
    @Transactional
    public Series createSeries(Long userId, SeriesCreateRequest request) {
        Series series = Series.builder()
                .userId(userId)
                .name(request.getName())
                .specTier(request.getSpecTier())
                .sizeTier(request.getSizeTier())
                .designStatus(DesignStatus.DESIGNING)
                .build();
        series = seriesRepository.save(series);
        log.info("创建系列: id={}, userId={}, specTier={}", series.getId(), userId, series.getSpecTier());
        return series;
    }

    /**
     * 获取系列详情(含画布列表)
     * 校验系列归属权
     */
    public SeriesDetailResponse getSeriesDetail(Long seriesId, Long userId) {
        Series series = getSeriesByIdAndUserId(seriesId, userId);

        List<Canvas> canvases = canvasRepository.findBySeriesIdOrderByCreatedAtAsc(seriesId);
        long finalizedCount = canvasRepository.countBySeriesIdAndStatus(seriesId,
                com.diyfigure.common.enums.CanvasStatus.FINALIZED);

        List<SeriesDetailResponse.CanvasSummary> canvasSummaries = canvases.stream()
                .map(this::toCanvasSummary)
                .collect(Collectors.toList());

        return SeriesDetailResponse.builder()
                .id(series.getId())
                .userId(series.getUserId())
                .name(series.getName())
                .specTier(series.getSpecTier())
                .sizeTier(series.getSizeTier())
                .designStatus(series.getDesignStatus())
                .createdAt(series.getCreatedAt())
                .updatedAt(series.getUpdatedAt())
                .canvases(canvasSummaries)
                .finalizedCount(finalizedCount)
                .totalCanvasCount((long) canvases.size())
                .build();
    }

    /**
     * 查询用户的所有系列
     */
    public List<Series> listByUserId(Long userId) {
        return seriesRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 更新系列(仅名称和尺寸档位,规格档位不可更改)
     */
    @Transactional
    public Series updateSeries(Long seriesId, Long userId, SeriesUpdateRequest request) {
        Series series = getSeriesByIdAndUserId(seriesId, userId);
        series.setName(request.getName());
        series.setSizeTier(request.getSizeTier());
        return seriesRepository.save(series);
    }

    /**
     * 删除系列(级联删除画布)
     * 注意:如果系列已进入交易流程(有关联订单),不允许删除
     */
    @Transactional
    public void deleteSeries(Long seriesId, Long userId) {
        Series series = getSeriesByIdAndUserId(seriesId, userId);
        // TODO: Phase 4 添加检查:如果系列已有关联订单,禁止删除
        List<Canvas> canvases = canvasRepository.findBySeriesIdOrderByCreatedAtAsc(seriesId);
        canvasRepository.deleteAll(canvases);
        seriesRepository.delete(series);
        log.info("删除系列: id={}, userId={}", seriesId, userId);
    }

    // ===== 辅助方法 =====

    /**
     * 根据 ID 和 userId 查询系列,校验归属权
     */
    public Series getSeriesByIdAndUserId(Long seriesId, Long userId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new BusinessException(ResultCode.SERIES_NOT_FOUND));
        if (!series.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此系列");
        }
        return series;
    }

    /**
     * 更新系列设计状态(当已定稿画布数达到档位要求时)
     */
    @Transactional
    public void updateDesignStatusIfNeeded(Long seriesId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new BusinessException(ResultCode.SERIES_NOT_FOUND));

        long finalizedCount = canvasRepository.countBySeriesIdAndStatus(seriesId,
                com.diyfigure.common.enums.CanvasStatus.FINALIZED);
        int requiredCount = series.getSpecTier().getDesignCount();

        if (finalizedCount >= requiredCount) {
            series.setDesignStatus(DesignStatus.READY_FOR_QUOTE);
        } else {
            series.setDesignStatus(DesignStatus.DESIGNING);
        }
        seriesRepository.save(series);
    }

    private SeriesDetailResponse.CanvasSummary toCanvasSummary(Canvas canvas) {
        String firstImage = (canvas.getConceptImageUrls() != null && !canvas.getConceptImageUrls().isEmpty())
                ? canvas.getConceptImageUrls().get(0)
                : null;
        return SeriesDetailResponse.CanvasSummary.builder()
                .id(canvas.getId())
                .status(canvas.getStatus())
                .model3dUrl(canvas.getModel3dUrl())
                .firstConceptImage(firstImage)
                .locked(canvas.getLocked())
                .createdAt(canvas.getCreatedAt())
                .finalizedAt(canvas.getFinalizedAt())
                .build();
    }
}
