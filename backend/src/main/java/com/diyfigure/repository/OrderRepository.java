package com.diyfigure.repository;

import com.diyfigure.common.enums.OrderStatus;
import com.diyfigure.common.enums.OrderType;
import com.diyfigure.entity.OrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 订单 Repository
 *
 * 主订单与补购订单共用此 Repository,通过 orderType 区分
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * 查询用户的全部订单(按创建时间倒序)
     */
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 查询用户某类型的订单(主订单/补购订单)
     */
    List<OrderEntity> findByUserIdAndOrderTypeOrderByCreatedAtDesc(Long userId, OrderType orderType);

    /**
     * 查询某系列的主订单
     */
    OrderEntity findBySeriesIdAndOrderType(Long seriesId, OrderType orderType);

    /**
     * 查询某状态下的所有订单(运营后台按状态筛选)
     */
    List<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /**
     * 查询待终审的订单列表(REVIEWING 状态)
     */
    List<OrderEntity> findByStatus(OrderStatus status);

    /**
     * 查询某主订单下的所有补购订单
     */
    List<OrderEntity> findByParentOrderIdOrderByCreatedAtDesc(Long parentOrderId);

    /**
     * 统计某状态下的订单数
     */
    long countByStatus(OrderStatus status);

    /**
     * 按 ID 查询订单(含关联数据)
     */
    Optional<OrderEntity> findById(Long id);

    /**
     * 判断某系列是否已产生订单(用于删除保护)
     */
    boolean existsBySeriesId(Long seriesId);

    boolean existsByAddressId(Long addressId);

    boolean existsBySeriesIdAndOrderTypeAndStatusNotIn(Long seriesId, OrderType orderType,
                                                       Collection<OrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id")
    Optional<OrderEntity> findByIdForUpdate(@Param("id") Long id);
}
