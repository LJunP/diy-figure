package com.diyfigure.repository;

import com.diyfigure.entity.CancellationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 取消/违约记录 Repository
 */
@Repository
public interface CancellationLogRepository extends JpaRepository<CancellationLog, Long> {

    /**
     * 查询订单的取消记录
     */
    List<CancellationLog> findByOrderId(Long orderId);

    /**
     * 查询所有取消记录(运营后台查看违约记录)
     */
    List<CancellationLog> findAllByOrderByCancelledAtDesc();
}
