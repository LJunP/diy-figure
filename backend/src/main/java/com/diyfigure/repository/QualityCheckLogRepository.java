package com.diyfigure.repository;

import com.diyfigure.entity.QualityCheckLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 质检记录 Repository
 */
@Repository
public interface QualityCheckLogRepository extends JpaRepository<QualityCheckLog, Long> {

    /**
     * 查询订单的质检记录(可能有多次:质检不通过返工后再检)
     */
    List<QualityCheckLog> findByOrderIdOrderByCheckedAtDesc(Long orderId);
}
