package com.diyfigure.repository;

import com.diyfigure.entity.OrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单状态变更审计日志 Repository
 *
 * ★ 每次状态转移必须写入一条记录,这是 service 层的强制约束
 */
@Repository
public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, Long> {

    /**
     * 查询订单的完整状态变更历史(按时间正序,可追溯完整流转链路)
     */
    List<OrderStatusLog> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
