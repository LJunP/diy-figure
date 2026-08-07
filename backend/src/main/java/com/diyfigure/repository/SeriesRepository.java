package com.diyfigure.repository;

import com.diyfigure.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系列 Repository
 */
@Repository
public interface SeriesRepository extends JpaRepository<Series, Long> {

    /**
     * 查询用户的所有系列(按创建时间倒序)
     */
    List<Series> findByUserIdOrderByCreatedAtDesc(Long userId);
}
