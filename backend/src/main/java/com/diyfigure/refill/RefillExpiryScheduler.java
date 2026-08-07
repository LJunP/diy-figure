package com.diyfigure.refill;

import com.diyfigure.common.enums.LotteryResult;
import com.diyfigure.entity.OrderCanvas;
import com.diyfigure.repository.OrderCanvasRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 补购窗口到期扫描定时任务(对应 03-技术设计说明书.md 第 8 节)
 *
 * 每日凌晨 2:00 扫描 order_canvas 表:
 * - NOT_SELECTED 角色的 refill_available_until 已过期
 * - 过期后该角色不可补购(静默失效,不做自动通知)
 *
 * ★ 设计决策:补购是非强制功能,过期静默失效即可,不发送通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefillExpiryScheduler {

    private final OrderCanvasRepository orderCanvasRepository;

    /**
     * 每日凌晨 2:00 扫描过期的补购窗口
     *
     * cron: 秒 分 时 日 月 周
     * 0 0 2 * * ? = 每天 02:00:00 执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scanExpiredRefillWindows() {
        LocalDate today = LocalDate.now();
        log.info("开始扫描补购窗口到期: date={}", today);

        // 查询所有未中签且补购窗口已过期的记录
        List<OrderCanvas> expiredCanvases = orderCanvasRepository
                .findByLotteryResult(LotteryResult.NOT_SELECTED);

        int expiredCount = 0;
        for (OrderCanvas oc : expiredCanvases) {
            if (oc.getRefillAvailableUntil() != null
                    && oc.getRefillAvailableUntil().isBefore(today)) {
                // 窗口已过期,标记为不可补购
                // ★ 当前设计:refill_available_until 过期即不可补购,无需额外字段
                // RefillService 在创建补购时检查 refill_available_until.isBefore(today)
                expiredCount++;
            }
        }

        log.info("补购窗口扫描完成: 扫描 {} 条未中签记录,其中 {} 条已过期", expiredCanvases.size(), expiredCount);
    }
}
