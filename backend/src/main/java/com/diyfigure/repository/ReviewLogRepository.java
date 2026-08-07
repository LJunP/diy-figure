package com.diyfigure.repository;

import com.diyfigure.entity.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 人工终审记录 Repository
 */
@Repository
public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    /**
     * 查询订单的终审记录(按时间倒序,可能有多次:终审拒绝后重新提交)
     */
    List<ReviewLog> findByOrderIdOrderByReviewedAtDesc(Long orderId);
}
