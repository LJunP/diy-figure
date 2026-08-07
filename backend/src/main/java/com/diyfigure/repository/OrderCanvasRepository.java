package com.diyfigure.repository;

import com.diyfigure.common.enums.LotteryResult;
import com.diyfigure.entity.OrderCanvas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单-画布关联 Repository
 *
 * 用于管理抽奖结果和补购资格
 */
@Repository
public interface OrderCanvasRepository extends JpaRepository<OrderCanvas, Long> {

    /**
     * 查询订单下的所有画布关联(含抽奖结果)
     */
    List<OrderCanvas> findByOrderId(Long orderId);

    /**
     * 查询订单中某抽奖结果的画布(中签/未中签)
     */
    List<OrderCanvas> findByOrderIdAndLotteryResult(Long orderId, LotteryResult lotteryResult);

    /**
     * 查询某画布在订单中的关联记录
     */
    List<OrderCanvas> findByCanvasId(Long canvasId);

    /**
     * 查询某抽奖结果的所有记录(定时任务扫描未中签角色用)
     */
    List<OrderCanvas> findByLotteryResult(LotteryResult lotteryResult);
}
