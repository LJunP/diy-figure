package com.diyfigure.repository;

import com.diyfigure.common.enums.Model3dStatus;
import com.diyfigure.entity.Canvas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 画布 Repository
 */
@Repository
public interface CanvasRepository extends JpaRepository<Canvas, Long> {

    /**
     * 查询系列下的所有画布(按创建时间正序)
     */
    List<Canvas> findBySeriesIdOrderByCreatedAtAsc(Long seriesId);

    /**
     * 统计系列下已定稿画布的数量(用于判断是否达到档位要求)
     */
    long countBySeriesIdAndStatus(Long seriesId, com.diyfigure.common.enums.CanvasStatus status);

    /**
     * 统计系列下画布总数
     */
    long countBySeriesId(Long seriesId);

    /**
     * 按 3D 生成状态捞取画布(异步生成调度用)
     */
    List<Canvas> findByModel3dStatus(com.diyfigure.common.enums.Model3dStatus status);
}
