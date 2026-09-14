package com.diyfigure.production;

import com.diyfigure.common.enums.OrderStatus;
import com.diyfigure.common.enums.OrderType;
import com.diyfigure.common.enums.PaymentChannel;
import com.diyfigure.common.enums.PaymentStatus;
import com.diyfigure.common.enums.PaymentType;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.OrderEntity;
import com.diyfigure.entity.Payment;
import com.diyfigure.order.service.OrderStateMachineService;
import com.diyfigure.production.dto.CancelResponse;
import com.diyfigure.repository.CancellationLogRepository;
import com.diyfigure.repository.OrderCanvasRepository;
import com.diyfigure.repository.OrderRepository;
import com.diyfigure.repository.PaymentRepository;
import com.diyfigure.repository.QualityCheckLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 取消违约金测试
 *
 * 覆盖 02 文档 4.4 节三档规则,重点防止两类回归:
 * 1. 待付定金(DEPOSIT_PENDING)时用户一分未付,却被按"已付定金"扣 30%
 * 2. 违约金按订单字段推算,而不是按真实支付金额计算
 */
@ExtendWith(MockitoExtension.class)
class CancellationFeeTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderCanvasRepository orderCanvasRepository;
    @Mock
    private QualityCheckLogRepository qcLogRepository;
    @Mock
    private CancellationLogRepository cancellationLogRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderStateMachineService stateMachineService;

    @InjectMocks
    private ProductionService productionService;

    private OrderEntity order(OrderStatus status, BigDecimal depositAmount, LocalDateTime startedAt) {
        return OrderEntity.builder()
                .id(1L)
                .userId(100L)
                .seriesId(1L)
                .orderType(OrderType.MAIN)
                .status(status)
                .depositAmount(depositAmount)
                .productionStartedAt(startedAt)
                .build();
    }

    private void stubNoDepositPaid() {
        when(paymentRepository.findByOrderIdAndType(eq(1L), eq(PaymentType.DEPOSIT)))
                .thenReturn(List.of());
    }

    private void stubDepositPaid(String amount) {
        when(paymentRepository.findByOrderIdAndType(eq(1L), eq(PaymentType.DEPOSIT)))
                .thenReturn(List.of(Payment.builder()
                        .orderId(1L)
                        .type(PaymentType.DEPOSIT)
                        .channel(PaymentChannel.WECHAT)
                        .amount(new BigDecimal(amount))
                        .status(PaymentStatus.SUCCESS)
                        .build()));
    }

    @Test
    @DisplayName("未付定金取消:不产生违约金(DEPOSIT_PENDING 用户尚未付款)")
    void cancelBeforeDepositPaid_noPenalty() {
        OrderEntity o = order(OrderStatus.DEPOSIT_PENDING, new BigDecimal("500.00"), null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        stubNoDepositPaid();

        CancelResponse res = productionService.cancelOrder(1L, 100L);

        assertEquals(0, BigDecimal.ZERO.compareTo(res.getDepositPenaltyAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(res.getDepositRefundAmount()));
    }

    @Test
    @DisplayName("已付定金未开工:扣实付定金 30%,退还 70%")
    void cancelAfterDepositNotStarted_thirtyPercentPenalty() {
        OrderEntity o = order(OrderStatus.IN_PRODUCTION, new BigDecimal("500.00"), null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        stubDepositPaid("1000.00");

        CancelResponse res = productionService.cancelOrder(1L, 100L);

        assertEquals(new BigDecimal("300.00"), res.getDepositPenaltyAmount());
        assertEquals(new BigDecimal("700.00"), res.getDepositRefundAmount());
    }

    @Test
    @DisplayName("按实付金额计算,而不是按订单上的定金字段")
    void penaltyUsesActuallyPaidAmount() {
        OrderEntity o = order(OrderStatus.IN_PRODUCTION, new BigDecimal("500.00"), null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        stubDepositPaid("200.00");

        CancelResponse res = productionService.cancelOrder(1L, 100L);

        assertEquals(new BigDecimal("60.00"), res.getDepositPenaltyAmount());
        assertEquals(new BigDecimal("140.00"), res.getDepositRefundAmount());
    }

    @Test
    @DisplayName("已开工取消:定金不退")
    void cancelAfterProductionStarted_noRefund() {
        OrderEntity o = order(OrderStatus.IN_PRODUCTION, new BigDecimal("500.00"), LocalDateTime.now());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        stubDepositPaid("1000.00");

        CancelResponse res = productionService.cancelOrder(1L, 100L);

        assertEquals(new BigDecimal("1000.00"), res.getDepositPenaltyAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(res.getDepositRefundAmount()));
    }

    @Test
    @DisplayName("不能取消他人的订单")
    void cancelOtherUsersOrder_forbidden() {
        OrderEntity o = order(OrderStatus.DEPOSIT_PENDING, null, null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productionService.cancelOrder(1L, 999L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("已发货订单不可取消")
    void cancelShippedOrder_rejected() {
        OrderEntity o = order(OrderStatus.SHIPPED, new BigDecimal("500.00"), LocalDateTime.now());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

        assertThrows(BusinessException.class, () -> productionService.cancelOrder(1L, 100L));
    }

    @Test
    @DisplayName("取消日志必须落库")
    void cancellationIsLogged() {
        OrderEntity o = order(OrderStatus.DEPOSIT_PENDING, null, null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        stubNoDepositPaid();

        productionService.cancelOrder(1L, 100L);

        org.mockito.Mockito.verify(cancellationLogRepository).save(any());
    }
}
