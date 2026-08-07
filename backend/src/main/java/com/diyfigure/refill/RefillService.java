package com.diyfigure.refill;

import com.diyfigure.common.enums.*;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.*;
import com.diyfigure.order.service.OrderStateMachineService;
import com.diyfigure.refill.dto.*;
import com.diyfigure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 补购服务
 *
 * 核心职责:
 * 1. 查看可补购角色:主订单已发货,未中签角色在补购窗口内(发货日+60天)
 * 2. 发起补购:创建 REFILL 类型订单,自动计算价格
 *    - 补购定价公式:(系列套餐总价 ÷ 中签数量) × 1.3
 *    - 跳过终审/报价/抽奖,从 DEPOSIT_PENDING 起步
 * 3. 校验补购窗口有效性:过期不可补购
 * 4. 校验不可重复补购:同一画布已补购不可再次补购
 *
 * ★ 补购订单复用 order 表(order_type=REFILL),状态机从 DEPOSIT_PENDING 起步
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefillService {

    private final OrderRepository orderRepository;
    private final OrderCanvasRepository orderCanvasRepository;
    private final CanvasRepository canvasRepository;
    private final SeriesRepository seriesRepository;
    private final OrderStateMachineService stateMachineService;

    /**
     * 补购定价倍数
     * 公式:(套餐总价 ÷ 中签数) × 1.3
     */
    private static final BigDecimal REFILL_PRICE_MULTIPLIER = new BigDecimal("1.3");

    /**
     * 查看可补购角色列表
     *
     * 前置条件:主订单已发货(SHIPPED 或 COMPLETED)
     * 返回未中签角色,标注是否过期和自动计算价格
     *
     * @param parentOrderId 主订单 ID
     * @param userId 用户 ID
     * @return 可补购角色列表
     */
    public List<RefillableCanvasResponse> getRefillableCanvases(Long parentOrderId, Long userId) {
        OrderEntity parentOrder = getOrderByIdAndUserId(parentOrderId, userId);

        // 校验主订单状态:必须已发货或已完成
        if (parentOrder.getStatus() != OrderStatus.SHIPPED
                && parentOrder.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "主订单未发货,暂不可补购");
        }

        // 获取未中签角色
        List<OrderCanvas> notSelected = orderCanvasRepository
                .findByOrderIdAndLotteryResult(parentOrderId, LotteryResult.NOT_SELECTED);

        // 计算补购单价
        BigDecimal refillPrice = calculateRefillPrice(parentOrder);

        // 查询已存在的补购订单(检查是否已补购)
        List<OrderEntity> refillOrders = orderRepository
                .findByParentOrderIdOrderByCreatedAtDesc(parentOrderId);
        Map<Long, OrderEntity> refilledCanvasMap = refillOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toMap(
                        o -> orderCanvasRepository.findByOrderId(o.getId())
                                .stream()
                                .map(OrderCanvas::getCanvasId)
                                .findFirst()
                                .orElse(0L),
                        o -> o,
                        (a, b) -> b
                ));

        // 获取画布信息
        List<Long> canvasIds = notSelected.stream()
                .map(OrderCanvas::getCanvasId)
                .collect(Collectors.toList());
        Map<Long, Canvas> canvasMap = canvasRepository.findAllById(canvasIds).stream()
                .collect(Collectors.toMap(Canvas::getId, c -> c));

        LocalDate today = LocalDate.now();

        return notSelected.stream()
                .map(oc -> {
                    Canvas canvas = canvasMap.get(oc.getCanvasId());
                    boolean expired = oc.getRefillAvailableUntil() != null
                            && oc.getRefillAvailableUntil().isBefore(today);
                    boolean alreadyRefilled = refilledCanvasMap.containsKey(oc.getCanvasId());

                    return RefillableCanvasResponse.builder()
                            .orderCanvasId(oc.getId())
                            .canvasId(oc.getCanvasId())
                            .canvasName(canvas != null ? "画布 " + canvas.getId() : "未知")
                            .firstConceptImage(canvas != null && canvas.getConceptImageUrls() != null
                                    && !canvas.getConceptImageUrls().isEmpty()
                                    ? canvas.getConceptImageUrls().get(0) : null)
                            .lotteryResult("NOT_SELECTED")
                            .refillAvailableUntil(oc.getRefillAvailableUntil())
                            .expired(expired || alreadyRefilled)
                            .refillPrice(refillPrice)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 发起补购
     *
     * 流程:
     * 1. 校验主订单已发货
     * 2. 校验画布属于该主订单且未中签
     * 3. 校验补购窗口未过期
     * 4. 校验未重复补购
     * 5. 计算补购价格:(套餐总价 ÷ 中签数) × 1.3
     * 6. 创建 REFILL 订单(DEPOSIT_PENDING)
     * 7. 创建 order_canvas 关联(单画布)
     * 8. 计算定金/尾款(各50%)
     *
     * @param parentOrderId 主订单 ID
     * @param userId 用户 ID
     * @param request 补购请求
     * @return 补购订单信息
     */
    @Transactional
    public RefillOrderResponse createRefillOrder(Long parentOrderId, Long userId, RefillRequest request) {
        OrderEntity parentOrder = getOrderByIdAndUserId(parentOrderId, userId);

        // 1. 校验主订单状态
        if (parentOrder.getStatus() != OrderStatus.SHIPPED
                && parentOrder.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "主订单未发货,暂不可补购");
        }

        // 2. 校验画布属于该主订单且未中签
        List<OrderCanvas> notSelected = orderCanvasRepository
                .findByOrderIdAndLotteryResult(parentOrderId, LotteryResult.NOT_SELECTED);
        OrderCanvas targetOc = notSelected.stream()
                .filter(oc -> oc.getCanvasId().equals(request.getCanvasId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.REFILL_NOT_AVAILABLE,
                        "该角色不在可补购列表中"));

        // 3. 校验补购窗口未过期
        if (targetOc.getRefillAvailableUntil() != null
                && targetOc.getRefillAvailableUntil().isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.REFILL_WINDOW_EXPIRED, "补购窗口已过期");
        }

        // 4. 校验未重复补购
        List<OrderEntity> existingRefills = orderRepository
                .findByParentOrderIdOrderByCreatedAtDesc(parentOrderId);
        for (OrderEntity refill : existingRefills) {
            if (refill.getStatus() != OrderStatus.CANCELLED) {
                List<OrderCanvas> refillCanvases = orderCanvasRepository.findByOrderId(refill.getId());
                boolean alreadyRefilled = refillCanvases.stream()
                        .anyMatch(oc -> oc.getCanvasId().equals(request.getCanvasId()));
                if (alreadyRefilled) {
                    throw new BusinessException(ResultCode.CONFLICT, "该角色已补购,不可重复补购");
                }
            }
        }

        // 5. 计算补购价格
        BigDecimal refillPrice = calculateRefillPrice(parentOrder);
        BigDecimal depositAmount = refillPrice.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal balanceAmount = refillPrice.subtract(depositAmount);

        // 6. 创建 REFILL 订单
        OrderEntity refillOrder = OrderEntity.builder()
                .seriesId(parentOrder.getSeriesId())
                .userId(userId)
                .orderType(OrderType.REFILL)
                .parentOrderId(parentOrderId)
                .status(OrderStatus.DEPOSIT_PENDING)
                .quotedPrice(refillPrice)
                .depositAmount(depositAmount)
                .balanceAmount(balanceAmount)
                .addressId(parentOrder.getAddressId()) // 复用主订单地址
                .build();
        refillOrder = orderRepository.save(refillOrder);

        // 7. 创建 order_canvas 关联(单画布,标记为 SELECTED)
        OrderCanvas refillOc = OrderCanvas.builder()
                .orderId(refillOrder.getId())
                .canvasId(request.getCanvasId())
                .lotteryResult(LotteryResult.SELECTED)
                .build();
        orderCanvasRepository.save(refillOc);

        log.info("创建补购订单: refillOrderId={}, parentOrderId={}, canvasId={}, price={}",
                refillOrder.getId(), parentOrderId, request.getCanvasId(), refillPrice);

        Canvas canvas = canvasRepository.findById(request.getCanvasId()).orElse(null);

        return RefillOrderResponse.builder()
                .refillOrderId(refillOrder.getId())
                .parentOrderId(parentOrderId)
                .canvasId(request.getCanvasId())
                .canvasName(canvas != null ? "画布 " + canvas.getId() : "未知")
                .refillPrice(refillPrice)
                .depositAmount(depositAmount)
                .balanceAmount(balanceAmount)
                .status(refillOrder.getStatus().name())
                .paymentUrl("/payment/" + refillOrder.getId())
                .build();
    }

    /**
     * 查询用户的所有补购订单
     */
    public List<OrderEntity> listRefillOrders(Long userId) {
        return orderRepository
                .findByUserIdAndOrderTypeOrderByCreatedAtDesc(userId, OrderType.REFILL);
    }

    // ===== 辅助方法 =====

    /**
     * 计算补购单价
     *
     * 公式:(套餐总价 ÷ 中签数) × 1.3
     * 套餐总价 = 主订单报价
     * 中签数 = 系列档位的 selectedCount (LIGHT:4, CLASSIC:6, COLLECTION:8)
     */
    private BigDecimal calculateRefillPrice(OrderEntity parentOrder) {
        if (parentOrder.getQuotedPrice() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "主订单未报价,无法计算补购价格");
        }

        Series series = seriesRepository.findById(parentOrder.getSeriesId())
                .orElseThrow(() -> new BusinessException(ResultCode.SERIES_NOT_FOUND));
        int selectedCount = series.getSpecTier().getSelectedCount();

        // (套餐总价 ÷ 中签数) × 1.3
        BigDecimal unitPrice = parentOrder.getQuotedPrice()
                .divide(new BigDecimal(selectedCount), 2, RoundingMode.HALF_UP);
        return unitPrice.multiply(REFILL_PRICE_MULTIPLIER)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private OrderEntity getOrderByIdAndUserId(Long orderId, Long userId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
        }
        return order;
    }
}
