package com.diyfigure.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 金额计算测试
 *
 * 核心不变量:拆分的各部分之和必须等于总额(不能出现 0.01 差额)
 */
class MoneyUtilsTest {

    @Test
    @DisplayName("偶数金额拆分:定金 + 尾款 == 总额")
    void half_evenTotal_sumsToTotal() {
        BigDecimal total = new BigDecimal("1000.00");
        BigDecimal deposit = MoneyUtils.half(total);
        BigDecimal balance = MoneyUtils.remainder(total, deposit);
        assertEquals(0, total.compareTo(deposit.add(balance)), "定金+尾款必须等于总额");
    }

    @Test
    @DisplayName("奇数金额拆分:不会出现重复进位导致的 0.01 差额")
    void half_oddTotal_sumsToTotal() {
        // 1000.01 / 2 = 500.005,HALF_UP 进位到 500.01;尾款由减法得到 500.00
        BigDecimal total = new BigDecimal("1000.01");
        BigDecimal deposit = MoneyUtils.half(total);
        BigDecimal balance = MoneyUtils.remainder(total, deposit);
        assertEquals(new BigDecimal("500.01"), deposit);
        assertEquals(new BigDecimal("500.00"), balance);
        assertEquals(0, total.compareTo(deposit.add(balance)));
    }

    @Test
    @DisplayName("补购价:先乘后除,只做一次舍入")
    void scaleThenMultiply_singleRounding() {
        // 报价 1000,中签 4 个 → 1000 * 1.3 / 4 = 325.00
        BigDecimal price = MoneyUtils.scaleThenMultiply(
                new BigDecimal("1000"),
                new BigDecimal("4"),
                new BigDecimal("1.3"));
        assertEquals(new BigDecimal("325.00"), price);

        // 先舍入单价(1001/6=166.85)再乘 1.3 会得到 216.91;
        // 一次性计算 1001*1.3/6 = 216.88,避免二次舍入
        BigDecimal price2 = MoneyUtils.scaleThenMultiply(
                new BigDecimal("1001"),
                new BigDecimal("6"),
                new BigDecimal("1.3"));
        assertEquals(new BigDecimal("216.88"), price2);
    }
}
