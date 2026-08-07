package com.diyfigure.common.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态枚举(核心状态机)
 *
 * 对应 02-产品需求说明书.md 第 4.2 节主状态机 + 第 4.3 节补购状态机
 * 对应 03-技术设计说明书.md 第 5.4.1 节 status 枚举
 *
 * 主订单 14 个状态节点 + CANCELLED(终态,可从多个节点触发):
 *   DRAFT_SUBMIT_PENDING → REVIEWING → QUOTED → LOTTERY_PENDING → LOTTERY_DONE
 *   → DEPOSIT_PENDING → IN_PRODUCTION → QC_PENDING → BALANCE_PENDING
 *   → SHIPPING_PENDING → SHIPPED → COMPLETED
 *
 * 补购订单(REFILL)状态机是子集,从 DEPOSIT_PENDING 起步,跳过终审/报价/抽奖环节
 *
 * ★ 关键约束:
 * 1. 「终审中」(REVIEWING) → 「已报价」(QUOTED) 必须由管理员手动调用审核接口,
 *    不可自动跳过,不可在代码里写默认通过逻辑
 * 2. 「生产中」(IN_PRODUCTION) 是单一状态,内部用 production_started_at 字段
 *    (03 文档字段名)区分罚金等级,不作为独立可见状态
 * 3. 所有状态转移必须经过 ALLOWED_TRANSITIONS 校验,不允许直接 UPDATE status
 */
public enum OrderStatus {

    // ===== 主订单状态(按生命周期顺序) =====

    /** 待提交报价(草稿阶段,画布设计中或已够数但未提交) */
    DRAFT_SUBMIT_PENDING,

    /** 终审中(运营人工审核设计合规性,硬性阻塞点) */
    REVIEWING,

    /** 终审拒绝(用户可修改画布后重新提交,回到 DRAFT_SUBMIT_PENDING) */
    REVIEW_REJECTED,

    /** 已报价(运营填写价格 + 预计交期,等待用户接受/拒绝) */
    QUOTED,

    /** 已关闭(用户拒绝报价,可重新打开修改后重新提交) */
    CLOSED,

    /** 待抽奖(用户接受报价,等待用户手动触发抽奖) */
    LOTTERY_PENDING,

    /** 已抽奖(系统按比例执行抽奖,等待用户填写收货地址) */
    LOTTERY_DONE,

    /** 待付定金(用户已填写收货地址,等待支付 50% 定金) */
    DEPOSIT_PENDING,

    /** 生产中(定金支付成功,设计锁定。内部用 production_started_at 区分罚金等级) */
    IN_PRODUCTION,

    /** 待质检(厂家生产完成,货送至平台,等待质检) */
    QC_PENDING,

    /** 待付尾款(质检通过,等待用户支付 50% 尾款) */
    BALANCE_PENDING,

    /** 待发货(尾款支付成功,等待平台发货) */
    SHIPPING_PENDING,

    /** 已发货(平台已发货,录入物流单号) */
    SHIPPED,

    /** 已完成(用户签收或运营标记完成) */
    COMPLETED,

    /** 已取消(终态,可从多个状态触发,违约金根据触发时的状态分段计算) */
    CANCELLED;

    // ===== 状态机转换规则(对应 03-技术设计说明书.md 第 6 节) =====

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry(DRAFT_SUBMIT_PENDING, EnumSet.of(REVIEWING, CANCELLED)),
            Map.entry(REVIEWING, EnumSet.of(REVIEW_REJECTED, QUOTED, CANCELLED)),
            // 终审拒绝后用户修改画布可重新提交,回到待提交报价
            Map.entry(REVIEW_REJECTED, EnumSet.of(DRAFT_SUBMIT_PENDING)),
            // 已报价:用户可接受(→待抽奖)或拒绝(→已关闭)
            Map.entry(QUOTED, EnumSet.of(CLOSED, LOTTERY_PENDING, CANCELLED)),
            // 已关闭:支持 02 文档 4.5 节,可重新打开
            Map.entry(CLOSED, EnumSet.of(DRAFT_SUBMIT_PENDING)),
            // 待抽奖:用户手动触发抽奖(不可自动执行)
            Map.entry(LOTTERY_PENDING, EnumSet.of(LOTTERY_DONE, CANCELLED)),
            // 已抽奖:用户填写收货地址后进入待付定金
            Map.entry(LOTTERY_DONE, EnumSet.of(DEPOSIT_PENDING, CANCELLED)),
            // 待付定金:支付成功→生产中,或取消(未付定金无损失)
            Map.entry(DEPOSIT_PENDING, EnumSet.of(IN_PRODUCTION, CANCELLED)),
            // 生产中:质检(待质检)或取消(违约金根据 production_started_at 判定)
            Map.entry(IN_PRODUCTION, EnumSet.of(QC_PENDING, CANCELLED)),
            // 待质检:质检通过→待付尾款,质检不通过→返工(回到生产中)
            Map.entry(QC_PENDING, EnumSet.of(IN_PRODUCTION, BALANCE_PENDING)),
            // 待付尾款:支付成功→待发货
            Map.entry(BALANCE_PENDING, EnumSet.of(SHIPPING_PENDING)),
            // 待发货:平台发货→已发货
            Map.entry(SHIPPING_PENDING, EnumSet.of(SHIPPED)),
            // 已发货:用户签收→已完成
            Map.entry(SHIPPED, EnumSet.of(COMPLETED)),
            // 终态:无后续转换
            Map.entry(COMPLETED, EnumSet.noneOf(OrderStatus.class)),
            Map.entry(CANCELLED, EnumSet.noneOf(OrderStatus.class))
    );

    /**
     * 判断从当前状态是否可以转换到目标状态
     *
     * @param to 目标状态
     * @return true=允许转换, false=非法转换
     */
    public boolean canTransitionTo(OrderStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrderStatus.class)).contains(to);
    }

    /**
     * 获取当前状态允许转换的所有目标状态
     */
    public Set<OrderStatus> getAllowedTransitions() {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrderStatus.class));
    }

    /**
     * 判断是否为终态(不可再转换)
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * 判断是否为补购订单的合法状态
     * 补购跳过 AI 设计、终审、报价、抽奖环节,从 DEPOSIT_PENDING 起步
     */
    public static boolean isValidRefillStatus(OrderStatus status) {
        return EnumSet.of(
                DEPOSIT_PENDING,
                IN_PRODUCTION,
                QC_PENDING,
                BALANCE_PENDING,
                SHIPPING_PENDING,
                SHIPPED,
                COMPLETED,
                CANCELLED
        ).contains(status);
    }
}
