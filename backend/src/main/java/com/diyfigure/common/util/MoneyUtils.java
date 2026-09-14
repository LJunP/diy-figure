package com.diyfigure.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额计算工具
 *
 * V1 统一规则:
 * - 金额精度 2 位小数,舍入模式 HALF_UP
 * - 拆分金额(定金/尾款)必须保证 "各部分之和 == 总额",
 *   因此只用一次舍入确定第一部分,剩余部分用减法得出,避免 0.01 的差额
 */
public final class MoneyUtils {

    private MoneyUtils() {
    }

    /**
     * 将总额按 1:1 拆成两笔,返回第一笔(定金)
     * 第二笔(尾款)请用 {@link #remainder(BigDecimal, BigDecimal)} 计算
     */
    public static BigDecimal half(BigDecimal total) {
        if (total == null) {
            return null;
        }
        return total.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算拆分后的剩余部分,保证 part + remainder == total
     */
    public static BigDecimal remainder(BigDecimal total, BigDecimal part) {
        if (total == null || part == null) {
            return null;
        }
        return total.subtract(part).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 按比例计算金额(只做一次舍入)
     *
     * @param total   基数
     * @param divisor 除数
     * @param multiplier 倍数
     */
    public static BigDecimal scaleThenMultiply(BigDecimal total, BigDecimal divisor, BigDecimal multiplier) {
        return total.multiply(multiplier)
                .divide(divisor, 2, RoundingMode.HALF_UP);
    }
}
