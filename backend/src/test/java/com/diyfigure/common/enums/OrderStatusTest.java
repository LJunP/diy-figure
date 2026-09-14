package com.diyfigure.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单状态机规则测试
 *
 * 这些是业务红线,任何改动都应在此处体现:
 * - 终审必须人工触发,不能跳过
 * - 抽奖必须由用户触发(已报价 → 待抽奖)
 * - 终态不可再流转
 */
class OrderStatusTest {

    @Test
    @DisplayName("终审是硬性阻塞点:待提交不能直接进入已报价")
    void reviewIsMandatory() {
        assertFalse(OrderStatus.DRAFT_SUBMIT_PENDING.canTransitionTo(OrderStatus.QUOTED));
        assertTrue(OrderStatus.DRAFT_SUBMIT_PENDING.canTransitionTo(OrderStatus.REVIEWING));
        assertTrue(OrderStatus.REVIEWING.canTransitionTo(OrderStatus.QUOTED));
        assertTrue(OrderStatus.REVIEWING.canTransitionTo(OrderStatus.REVIEW_REJECTED));
    }

    @Test
    @DisplayName("终审拒绝后可重新提交:REVIEW_REJECTED → DRAFT_SUBMIT_PENDING")
    void rejectedCanResubmit() {
        assertTrue(OrderStatus.REVIEW_REJECTED.canTransitionTo(OrderStatus.DRAFT_SUBMIT_PENDING));
        assertFalse(OrderStatus.REVIEW_REJECTED.canTransitionTo(OrderStatus.REVIEWING));
    }

    @Test
    @DisplayName("拒绝报价后可重新打开:CLOSED → DRAFT_SUBMIT_PENDING")
    void closedCanReopen() {
        assertTrue(OrderStatus.CLOSED.canTransitionTo(OrderStatus.DRAFT_SUBMIT_PENDING));
        assertFalse(OrderStatus.CLOSED.canTransitionTo(OrderStatus.QUOTED));
    }

    @Test
    @DisplayName("终态不可再流转")
    void terminalStates() {
        assertTrue(OrderStatus.COMPLETED.isTerminal());
        assertTrue(OrderStatus.CANCELLED.isTerminal());
        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("待付定金可以取消(此时用户尚未付款,不应产生违约金)")
    void depositPendingCanCancel() {
        assertTrue(OrderStatus.DEPOSIT_PENDING.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.IN_PRODUCTION.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("已发货后不允许取消(只能签收完成)")
    void shippedCannotCancel() {
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    @DisplayName("质检不通过回到生产中返工")
    void qcFailedReturnsToProduction() {
        assertTrue(OrderStatus.QC_PENDING.canTransitionTo(OrderStatus.IN_PRODUCTION));
        assertTrue(OrderStatus.QC_PENDING.canTransitionTo(OrderStatus.BALANCE_PENDING));
    }
}
