package com.diyfigure.order.service;

import com.diyfigure.common.enums.*;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.*;
import com.diyfigure.order.dto.*;
import com.diyfigure.repository.*;
import com.diyfigure.common.util.MoneyUtils;
import com.diyfigure.series.SeriesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务
 *
 * 核心职责:
 * 1. 创建订单(主订单): 系列 → 订单 → order_canvas 关联
 * 2. 终审(运营端): REVIEWING → QUOTED / REVIEW_REJECTED
 * 3. 报价(运营端): 填写价格 → 通知用户
 * 4. 抽奖(用户侧): 随机抽选中签画布 → 更新 order_canvas
 * 5. 接受/拒绝报价(用户侧): QUOTED → LOTTERY_PENDING / CLOSED
 *
 * ★ 关键约束:
 * - 终审是硬性阻塞点,必须由管理员手动审核,不可自动跳过
 * - 抽奖必须由用户手动触发,不可在创建订单或支付成功时自动执行
 * - 所有状态转移通过 OrderStateMachineService.transition() 统一处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderCanvasRepository orderCanvasRepository;
    private final OrderStateMachineService stateMachineService;
    private final CanvasRepository canvasRepository;
    private final SeriesRepository seriesRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final SeriesService seriesService;
    private final AddressRepository addressRepository;
    private final com.diyfigure.repository.UserRepository userRepository;

    /**
     * 创建主订单
     *
     * 流程:
     * 1. 校验系列归属权和设计状态(必须是 READY_FOR_QUOTE)
     * 2. 校验画布归属权和定稿状态
     * 3. 校验画布数量是否达到档位要求(LIGHT:6, CLASSIC:9, COLLECTION:12)
     * 4. 创建 order 记录(状态: DRAFT_SUBMIT_PENDING)
     * 5. 创建 order_canvas 记录(状态: DRAFT_SUBMIT_PENDING, lottery_result 暂不设置)
     * 6. 状态转移: DRAFT_SUBMIT_PENDING → REVIEWING
     *
     * @param userId 用户 ID
     * @param request 创建订单请求
     * @return 订单详情
     */
    @Transactional
    public OrderDetailResponse createMainOrder(Long userId, OrderCreateRequest request) {
        // 1. 校验系列归属权和设计状态
        Series series = seriesService.getSeriesByIdAndUserId(request.getSeriesId(), userId);
        if (series.getDesignStatus() != DesignStatus.READY_FOR_QUOTE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "系列尚未达到可提交报价状态");
        }
        if (orderRepository.existsBySeriesIdAndOrderTypeAndStatusNotIn(
                series.getId(), OrderType.MAIN,
                java.util.EnumSet.of(OrderStatus.CANCELLED, OrderStatus.CLOSED))) {
            throw new BusinessException(ResultCode.CONFLICT, "该系列已有进行中的订单,请使用原订单继续");
        }

        // 2. 校验画布归属权和定稿状态
        List<Canvas> canvases = canvasRepository.findAllById(request.getCanvasIds());
        if (canvases.size() != request.getCanvasIds().size()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部分画布不存在");
        }

        // 校验画布是否都属于该系列且已定稿
        for (Canvas canvas : canvases) {
            if (!canvas.getSeriesId().equals(series.getId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "画布不属于该系列");
            }
            if (canvas.getStatus() != CanvasStatus.FINALIZED) {
                throw new BusinessException(ResultCode.CANVAS_NOT_FINALIZED,
                        "画布「" + canvas.getId() + "」未定稿");
            }
        }

        // 3. 校验画布数量是否达到档位要求
        int requiredCount = series.getSpecTier().getDesignCount();
        if (canvases.size() < requiredCount) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "画布数量不足,该档位需要 " + requiredCount + " 个已定稿画布");
        }

        // 4. 创建订单
        OrderEntity order = OrderEntity.builder()
                .seriesId(series.getId())
                .userId(userId)
                .orderType(OrderType.MAIN)
                .status(OrderStatus.DRAFT_SUBMIT_PENDING)
                .build();
        order = orderRepository.save(order);

        // 5. 创建 order_canvas 关联
        for (Canvas canvas : canvases) {
            OrderCanvas orderCanvas = OrderCanvas.builder()
                    .orderId(order.getId())
                    .canvasId(canvas.getId())
                    .lotteryResult(LotteryResult.UNDECIDED)
                    .build();
            orderCanvasRepository.save(orderCanvas);
        }

        // 6. 状态转移: DRAFT_SUBMIT_PENDING → REVIEWING
        stateMachineService.transition(order, OrderStatus.REVIEWING,
                OperatorType.USER, userId, "提交报价申请");

        log.info("创建主订单: orderId={}, seriesId={}, canvasCount={}",
                order.getId(), series.getId(), canvases.size());

        return getOrderDetail(order.getId(), userId);
    }

    /**
     * 运营端: 终审
     *
     * ★ 硬性阻塞点:必须由管理员手动调用,不可自动跳过
     *
     * @param orderId 订单 ID
     * @param reviewerId 审核人 ID(运营账号)
     * @param request 审核请求
     */
    @Transactional
    public void reviewOrder(Long orderId, Long reviewerId, ReviewRequest request) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));

        // 校验当前状态必须是 REVIEWING
        if (order.getStatus() != OrderStatus.REVIEWING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待审核状态");
        }

        ReviewResult result;
        try {
            result = ReviewResult.valueOf(request.getResult());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "审核结果必须是 APPROVED 或 REJECTED");
        }
        if (result == ReviewResult.REJECTED
                && (request.getRejectReason() == null || request.getRejectReason().isBlank())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "拒绝时必须填写理由");
        }

        ReviewLog reviewLog = ReviewLog.builder()
                .orderId(orderId)
                .reviewerId(reviewerId)
                .result(result)
                .rejectReason(request.getRejectReason())
                .reviewedAt(java.time.LocalDateTime.now())
                .build();
        reviewLogRepository.save(reviewLog);

        if (result == ReviewResult.APPROVED) {
            stateMachineService.transition(order, OrderStatus.QUOTED,
                    OperatorType.ADMIN, reviewerId, "终审通过");
        } else {
            stateMachineService.transition(order, OrderStatus.REVIEW_REJECTED,
                    OperatorType.ADMIN, reviewerId, "终审拒绝: " + request.getRejectReason());
        }
    }

    /**
     * 运营端: 报价
     *
     * 终审通过后,运营填写报价和预计交期
     *
     * @param orderId 订单 ID
     * @param reviewerId 运营 ID
     * @param request 报价请求
     */
    @Transactional
    public void quoteOrder(Long orderId, Long reviewerId, QuoteRequest request) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));

        // 校验当前状态必须是 QUOTED
        if (order.getStatus() != OrderStatus.QUOTED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待报价状态");
        }

        // 更新报价信息
        order.setQuotedPrice(request.getQuotedPrice());
        if (request.getExpectedDeliveryDate() != null) {
            order.setExpectedDeliveryDate(LocalDate.parse(request.getExpectedDeliveryDate()));
        }
        orderRepository.save(order);

        log.info("订单报价: orderId={}, price={}", orderId, request.getQuotedPrice());
    }

    /**
     * 用户端: 接受报价
     *
     * QUOTED → LOTTERY_PENDING
     */
    @Transactional
    public void acceptQuote(Long orderId, Long userId) {
        OrderEntity order = getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.QUOTED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待接受报价状态");
        }

        // 前置检查:终审通过后即进入 QUOTED,此时报价可能还没填。
        // 没有有效价格就允许接受,后续的定金/尾款金额会算成 0,必须在这里挡住。
        if (order.getQuotedPrice() == null || order.getQuotedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报价尚未填写,不能接受报价");
        }

        stateMachineService.transition(order, OrderStatus.LOTTERY_PENDING,
                OperatorType.USER, userId, "接受报价");
    }

    /**
     * 用户端: 拒绝报价
     *
     * QUOTED → CLOSED
     */
    @Transactional
    public void rejectQuote(Long orderId, Long userId) {
        OrderEntity order = getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.QUOTED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待接受报价状态");
        }

        stateMachineService.transition(order, OrderStatus.CLOSED,
                OperatorType.USER, userId, "拒绝报价");
    }

    /**
     * 用户端: 终审拒绝后重新提交
     *
     * REVIEW_REJECTED → DRAFT_SUBMIT_PENDING
     * 用户按拒绝理由修改设计后,重新进入待提交报价,可再次提交终审。
     */
    @Transactional
    public void resubmitOrder(Long orderId, Long userId) {
        OrderEntity order = getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.REVIEW_REJECTED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在终审拒绝状态");
        }

        stateMachineService.transition(order, OrderStatus.DRAFT_SUBMIT_PENDING,
                OperatorType.USER, userId, "终审拒绝后重新提交");

        log.info("订单重新提交: orderId={}, userId={}", orderId, userId);
    }

    /**
     * 用户端: 重新打开已关闭的订单(02 文档 4.5 节)
     *
     * CLOSED → DRAFT_SUBMIT_PENDING
     * 用户拒绝报价后,可重新打开订单修改设计并再次申请报价。
     */
    @Transactional
    public void reopenOrder(Long orderId, Long userId) {
        OrderEntity order = getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.CLOSED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在已关闭状态");
        }

        stateMachineService.transition(order, OrderStatus.DRAFT_SUBMIT_PENDING,
                OperatorType.USER, userId, "重新打开订单");

        log.info("订单重新打开: orderId={}, userId={}", orderId, userId);
    }

    /**
     * 用户端: 提交终审
     *
     * DRAFT_SUBMIT_PENDING → REVIEWING
     *
     * 返工闭环的最后一环。resubmit / reopen 都只把订单放回 DRAFT_SUBMIT_PENDING,
     * 如果没有这个入口,订单会永久卡在草稿态,再也进不了终审队列。
     *
     * 前置检查:画布必须仍然满足档位数量要求且已定稿,
     * 否则会把一份不合规的设计重新塞进运营的终审队列。
     */
    @Transactional
    public void submitForReview(Long orderId, Long userId) {
        OrderEntity order = getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.DRAFT_SUBMIT_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待提交状态");
        }

        Series series = seriesRepository.findById(order.getSeriesId())
                .orElseThrow(() -> new BusinessException(ResultCode.SERIES_NOT_FOUND));

        List<OrderCanvas> orderCanvases = orderCanvasRepository.findByOrderId(orderId);
        List<Long> canvasIds = orderCanvases.stream()
                .map(OrderCanvas::getCanvasId)
                .collect(Collectors.toList());
        List<Canvas> canvases = canvasRepository.findAllById(canvasIds);

        if (canvases.size() < series.getSpecTier().getDesignCount()) {
            throw new BusinessException(ResultCode.FINALIZED_COUNT_NOT_ENOUGH,
                    "画布数量不足,该档位需要 " + series.getSpecTier().getDesignCount() + " 个已定稿画布");
        }
        for (Canvas canvas : canvases) {
            if (canvas.getStatus() != CanvasStatus.FINALIZED) {
                throw new BusinessException(ResultCode.CANVAS_NOT_FINALIZED,
                        "画布「" + canvas.getId() + "」未定稿");
            }
        }

        stateMachineService.transition(order, OrderStatus.REVIEWING,
                OperatorType.USER, userId, "重新提交终审");

        log.info("订单提交终审: orderId={}, userId={}", orderId, userId);
    }

    /**
     * 用户端: 执行抽奖
     *
     * ★ 关键约束:必须由用户手动触发,不可自动执行
     *
     * 流程:
     * 1. 校验状态必须是 LOTTERY_PENDING
     * 2. 获取系列的规格档位,确定选中数量(LIGHT:4, CLASSIC:6, COLLECTION:8)
     * 3. 随机抽中指定数量的画布
     * 4. 更新 order_canvas.lottery_result (SELECTED / NOT_SELECTED)
     * 5. 未中签角色的 refill_available_until = 发货日 + 60天(发货时更新,当前留 null)
     * 6. 状态转移: LOTTERY_PENDING → LOTTERY_DONE
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID
     * @return 抽奖结果(中签/未中签画布列表)
     */
    @Transactional
    public Map<String, List<Canvas>> drawLottery(Long orderId, Long userId) {
        OrderEntity order = getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.LOTTERY_PENDING) {
            throw new BusinessException(ResultCode.LOTTERY_ALREADY_DONE, "订单不在待抽奖状态");
        }

        // 获取系列的规格档位
        Series series = seriesRepository.findById(order.getSeriesId())
                .orElseThrow(() -> new BusinessException(ResultCode.SERIES_NOT_FOUND));
        int selectedCount = series.getSpecTier().getSelectedCount();

        // 获取所有参与抽奖的画布
        List<OrderCanvas> orderCanvases = orderCanvasRepository.findByOrderId(orderId);
        if (orderCanvases.size() < selectedCount) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "画布数量不足,无法抽奖");
        }

        // 随机抽中指定数量的画布
        List<OrderCanvas> allCanvases = new ArrayList<>(orderCanvases);
        Collections.shuffle(allCanvases);
        List<OrderCanvas> selectedCanvases = allCanvases.subList(0, selectedCount);
        List<OrderCanvas> notSelectedCanvases = allCanvases.subList(selectedCount, allCanvases.size());

        // 更新抽奖结果
        for (OrderCanvas oc : selectedCanvases) {
            oc.setLotteryResult(LotteryResult.SELECTED);
            oc.setRefillAvailableUntil(null); // 中签角色无补购截止日期
        }
        for (OrderCanvas oc : notSelectedCanvases) {
            oc.setLotteryResult(LotteryResult.NOT_SELECTED);
            // 补购截止日期在发货时更新(当前留 null)
        }
        orderCanvasRepository.saveAll(orderCanvases);

        // 状态转移: LOTTERY_PENDING → LOTTERY_DONE
        stateMachineService.transition(order, OrderStatus.LOTTERY_DONE,
                OperatorType.USER, userId, "完成抽奖");

        log.info("抽奖完成: orderId={}, selected={}, notSelected={}",
                orderId, selectedCanvases.size(), notSelectedCanvases.size());

        // 返回中签/未中签画布信息
        Map<String, List<Canvas>> result = new HashMap<>();
        result.put("selected", getCanvasesById(
                selectedCanvases.stream().map(OrderCanvas::getCanvasId).collect(Collectors.toList())
        ));
        result.put("notSelected", getCanvasesById(
                notSelectedCanvases.stream().map(OrderCanvas::getCanvasId).collect(Collectors.toList())
        ));
        return result;
    }

    /**
     * 获取订单详情
     */
    public OrderDetailResponse getOrderDetail(Long orderId, Long userId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));

        // 校验归属权(订单属于当前用户 或是运营管理员)
        if (!order.getUserId().equals(userId)) {
            // 检查是否为管理员
            com.diyfigure.entity.User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getRole() != com.diyfigure.common.enums.UserRole.ADMIN) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权查看此订单");
            }
        }

        // 获取系列信息
        Series series = seriesRepository.findById(order.getSeriesId())
                .orElse(null);

        // 获取画布信息
        List<OrderCanvas> orderCanvases = orderCanvasRepository.findByOrderId(orderId);
        List<Long> canvasIds = orderCanvases.stream()
                .map(OrderCanvas::getCanvasId)
                .collect(Collectors.toList());
        Map<Long, Canvas> canvasMap = canvasRepository.findAllById(canvasIds).stream()
                .collect(Collectors.toMap(Canvas::getId, c -> c));

        List<OrderDetailResponse.CanvasInfo> canvasInfos = orderCanvases.stream()
                .map(oc -> {
                    Canvas c = canvasMap.get(oc.getCanvasId());
                    if (c == null) {
                        return OrderDetailResponse.CanvasInfo.builder()
                                .canvasId(oc.getCanvasId())
                                .name("画布 " + oc.getCanvasId())
                                .lotteryResult(oc.getLotteryResult() != null ? oc.getLotteryResult().name() : null)
                                .build();
                    }
                    String firstImage = (c.getConceptImageUrls() != null && !c.getConceptImageUrls().isEmpty())
                            ? c.getConceptImageUrls().get(0) : null;
                    return OrderDetailResponse.CanvasInfo.builder()
                            .canvasId(c.getId())
                            .name(canvasDisplayName(c))
                            .status(c.getStatus().name())
                            .firstConceptImage(firstImage)
                            .lotteryResult(oc.getLotteryResult() != null ? oc.getLotteryResult().name() : null)
                            .refillAvailableUntil(oc.getRefillAvailableUntil() != null
                                    ? oc.getRefillAvailableUntil().toString() : null)
                            .build();
                })
                .collect(Collectors.toList());

        OrderDetailResponse.AddressInfo addressInfo = null;
        if (order.getAddressId() != null) {
            Address bound = addressRepository.findById(order.getAddressId()).orElse(null);
            if (bound != null) {
                addressInfo = OrderDetailResponse.AddressInfo.builder()
                        .id(bound.getId())
                        .receiverName(bound.getReceiverName())
                        .phone(bound.getPhone())
                        .detail(bound.getDetail())
                        .build();
            }
        }

        return OrderDetailResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderType(order.getOrderType().name())
                .parentOrderId(order.getParentOrderId())
                .status(order.getStatus().name())
                .quotedPrice(order.getQuotedPrice())
                .depositAmount(order.getDepositAmount())
                .balanceAmount(order.getBalanceAmount())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .trackingNumber(order.getTrackingNumber())
                .trackingCompany(order.getTrackingCompany())
                .addressId(order.getAddressId())
                .address(addressInfo)
                .createdAt(order.getCreatedAt())
                // 终审拒绝时把最近一次拒绝理由带给用户,否则用户不知道要改什么
                .rejectReason(order.getStatus() == OrderStatus.REVIEW_REJECTED
                        ? findLatestRejectReason(orderId) : null)
                .series(series != null ? OrderDetailResponse.SeriesInfo.builder()
                        .id(series.getId())
                        .name(series.getName())
                        .specTier(series.getSpecTier().name())
                        .sizeTier(series.getSizeTier())
                        .build() : null)
                .canvases(canvasInfos)
                .build();
    }

    /**
     * 画布展示名
     *
     * name 是 NOT NULL 列,正常不会为空;这里兜底只是为了兼容
     * V5 迁移之前就存在、且没被回填到的历史数据。
     */
    private String canvasDisplayName(Canvas canvas) {
        return (canvas.getName() == null || canvas.getName().isBlank())
                ? "画布 " + canvas.getId()
                : canvas.getName();
    }

    /**
     * 查询订单最近一次终审拒绝理由
     */
    private String findLatestRejectReason(Long orderId) {
        return reviewLogRepository.findByOrderIdOrderByReviewedAtDesc(orderId).stream()
                .filter(log -> log.getResult() == ReviewResult.REJECTED)
                .map(com.diyfigure.entity.ReviewLog::getRejectReason)
                .filter(reason -> reason != null && !reason.isBlank())
                .findFirst()
                .orElse(null);
    }

    /**
     * 查询用户的所有主订单
     */
    public List<OrderEntity> listMainOrders(Long userId) {
        return orderRepository.findByUserIdAndOrderTypeOrderByCreatedAtDesc(userId, OrderType.MAIN);
    }

    /**
     * 用户端: 绑定收货地址
     *
     * LOTTERY_DONE → DEPOSIT_PENDING
     *
     * 抽奖完成后,用户必须填写收货地址才能进入定金支付环节
     * 同时计算定金/尾款金额(各50%)
     */
    @Transactional
    public void bindAddress(Long orderId, Long userId, Long addressId) {
        OrderEntity order = getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.LOTTERY_DONE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在待填写地址状态");
        }
        if (addressId == null) {
            throw new BusinessException(ResultCode.ADDRESS_REQUIRED);
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "地址不存在"));
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此地址");
        }

        // 绑定地址
        order.setAddressId(addressId);

        // 计算定金/尾款金额(各50%)
        // 尾款用减法得出,保证 定金 + 尾款 == 报价(避免 1000.01 拆成 500.01 + 500.01)
        if (order.getQuotedPrice() != null) {
            BigDecimal deposit = MoneyUtils.half(order.getQuotedPrice());
            order.setDepositAmount(deposit);
            order.setBalanceAmount(MoneyUtils.remainder(order.getQuotedPrice(), deposit));
        }
        orderRepository.save(order);

        // 状态转移: LOTTERY_DONE → DEPOSIT_PENDING
        stateMachineService.transition(order, OrderStatus.DEPOSIT_PENDING,
                OperatorType.USER, userId, "填写收货地址");

        log.info("绑定收货地址: orderId={}, addressId={}", orderId, addressId);
    }

    /**
     * 运营端: 送检(将生产中的订单推入待质检)
     * IN_PRODUCTION → QC_PENDING
     */
    @Transactional
    public void submitForQc(Long orderId, Long adminId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.IN_PRODUCTION) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单不在生产中状态");
        }

        stateMachineService.transition(order, OrderStatus.QC_PENDING,
                OperatorType.ADMIN, adminId, "提交质检");
    }

    // ===== 辅助方法 =====

    private OrderEntity getOrderByIdAndUserId(Long orderId, Long userId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
        }
        return order;
    }

    private List<Canvas> getCanvasesById(List<Long> canvasIds) {
        return canvasRepository.findAllById(canvasIds);
    }
}
