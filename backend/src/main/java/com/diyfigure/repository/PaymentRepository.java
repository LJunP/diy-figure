package com.diyfigure.repository;

import com.diyfigure.common.enums.PaymentStatus;
import com.diyfigure.common.enums.PaymentType;
import com.diyfigure.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 支付流水 Repository
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 查询订单的支付记录
     */
    List<Payment> findByOrderId(Long orderId);

    /**
     * 查询订单某类型的全部支付记录(定金/尾款)。
     * 不要用 Optional 单条:成功后再建一单会让单条查询抛 IncorrectResultSize。
     */
    List<Payment> findByOrderIdAndType(Long orderId, PaymentType type);

    /**
     * 查询某状态的支付记录
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * 根据支付网关交易号查询(用于回调核对)
     */
    Optional<Payment> findByChannelTransactionId(String channelTransactionId);
}
