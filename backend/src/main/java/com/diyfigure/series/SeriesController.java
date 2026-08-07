package com.diyfigure.series;

import com.diyfigure.auth.JwtInterceptor;
import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.entity.Series;
import com.diyfigure.series.dto.SeriesCreateRequest;
import com.diyfigure.series.dto.SeriesDetailResponse;
import com.diyfigure.series.dto.SeriesUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系列控制器
 *
 * 接口清单(对应 03-技术设计说明书.md 第 7.2 节):
 * - POST   /api/series           创建系列
 * - GET    /api/series           查询当前用户的所有系列
 * - GET    /api/series/{id}      系列详情(含画布列表)
 * - PUT    /api/series/{id}      更新系列
 * - DELETE /api/series/{id}      删除系列
 */
@RestController
@RequestMapping("/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;

    /**
     * 创建系列
     */
    @PostMapping
    public ApiResponse<Series> create(@Valid @RequestBody SeriesCreateRequest body,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(seriesService.createSeries(userId, body));
    }

    /**
     * 查询当前用户的所有系列
     */
    @GetMapping
    public ApiResponse<List<Series>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(seriesService.listByUserId(userId));
    }

    /**
     * 系列详情(含画布列表)
     */
    @GetMapping("/{id}")
    public ApiResponse<SeriesDetailResponse> detail(@PathVariable Long id,
                                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(seriesService.getSeriesDetail(id, userId));
    }

    /**
     * 更新系列
     */
    @PutMapping("/{id}")
    public ApiResponse<Series> update(@PathVariable Long id,
                                      @Valid @RequestBody SeriesUpdateRequest body,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(seriesService.updateSeries(id, userId, body));
    }

    /**
     * 删除系列
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        seriesService.deleteSeries(id, userId);
        return ApiResponse.success();
    }
}
