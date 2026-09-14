package com.diyfigure.integration;

import com.diyfigure.common.enums.*;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.*;
import com.diyfigure.canvas.CanvasService;
import com.diyfigure.canvas.dto.CanvasCreateRequest;
import com.diyfigure.order.dto.OrderCreateRequest;
import com.diyfigure.order.dto.OrderDetailResponse;
import com.diyfigure.order.dto.QuoteRequest;
import com.diyfigure.order.dto.ReviewRequest;
import com.diyfigure.order.service.OrderService;
import com.diyfigure.payment.PaymentService;
import com.diyfigure.payment.dto.PaymentCreateRequest;
import com.diyfigure.payment.dto.PaymentResponse;
import com.diyfigure.production.ProductionService;
import com.diyfigure.production.dto.CancelResponse;
import com.diyfigure.production.dto.QualityCheckRequest;
import com.diyfigure.production.dto.ShipRequest;
import com.diyfigure.refill.RefillExpiryScheduler;
import com.diyfigure.refill.RefillService;
import com.diyfigure.refill.dto.RefillOrderResponse;
import com.diyfigure.refill.dto.RefillRequest;
import com.diyfigure.refill.dto.RefillableCanvasResponse;
import com.diyfigure.notification.NotificationService;
import com.diyfigure.repository.*;
import com.diyfigure.series.SeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全流程回归测试
 *
 * 这是整个项目最重要的一组测试:它把「下单 → 终审 → 报价 → 抽奖 → 支付 → 生产 →
 * 质检 → 发货 → 完成」主链路,以及返工 / 取消违约金 / 补购 / 补购窗口过期 / 关闭重开
 * 等分支,全部串起来跑一遍。任何一处状态机、金额、权限判断被改坏,这里立刻会红。
 *
 * 运行前提:
 * 1. MySQL 上存在独立 schema diy_figure_it(见 src/test/resources/application-test.yml 顶部注释)
 * 2. 每个用例执行前由 Flyway clean + migrate 重建表结构,用例之间互不干扰
 * 3. ddl-auto=validate —— 本组测试同时是「Flyway 脚本与 JPA 实体是否一致」的守卫
 *
 * 不使用 @Transactional 回滚做隔离:service 内部事务参与外层事务失败时会被标记
 * rollback-only,导致同一用例里后续的断言读到脏状态。改用整库重建,语义更干净。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("全流程回归测试")
class OrderFlowIntegrationTest {

    // ===== 依赖 =====
    @Autowired private OrderService orderService;
    @Autowired private ProductionService productionService;
    @Autowired private PaymentService paymentService;
    @Autowired private RefillService refillService;
    @Autowired private NotificationService notificationService;
    @Autowired private SeriesService seriesService;
    @Autowired private CanvasService canvasService;
    @Autowired private RefillExpiryScheduler refillExpiryScheduler;

    @Autowired private UserRepository userRepository;
    @Autowired private SeriesRepository seriesRepository;
    @Autowired private CanvasRepository canvasRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderCanvasRepository orderCanvasRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OrderStatusLogRepository orderStatusLogRepository;
    @Autowired private CancellationLogRepository cancellationLogRepository;
    @Autowired private ReviewLogRepository reviewLogRepository;

    @Autowired private org.flywaydb.core.Flyway flyway;

    /** 轻量档:设计 6 个、中签 4 个、未中签 2 个 */
    private static final SpecTier TIER = SpecTier.LIGHT;
    private static final BigDecimal PRICE = new BigDecimal("1000.00");

    private Long userId;
    private Long adminId;
    private Long seriesId;
    private Long addressId;
    private List<Long> canvasIds;
    private List<String> canvasNames;

    @BeforeEach
    void resetAndSeed() {
        flyway.clean();
        flyway.migrate();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User user = userRepository.save(User.builder()
                .username("u" + suffix)
                .email("u" + suffix + "@test.local")
                .passwordHash("not-a-real-hash")
                .role(UserRole.USER)
                .build());
        User admin = userRepository.save(User.builder()
                .username("a" + suffix)
                .email("a" + suffix + "@test.local")
                .passwordHash("not-a-real-hash")
                .role(UserRole.ADMIN)
                .build());
        userId = user.getId();
        adminId = admin.getId();

        Series series = seriesRepository.save(Series.builder()
                .userId(userId)
                .name("回归测试系列")
                .specTier(TIER)
                .sizeTier("STANDARD")
                .designStatus(DesignStatus.READY_FOR_QUOTE)
                .build());
        seriesId = series.getId();

        canvasIds = new ArrayList<>();
        canvasNames = new ArrayList<>();
        for (int i = 0; i < TIER.getDesignCount(); i++) {
            String canvasName = "测试角色 " + (i + 1);
            Canvas c = canvasRepository.save(Canvas.builder()
                    .seriesId(seriesId)
                    .name(canvasName)
                    .status(CanvasStatus.FINALIZED)
                    .conceptImageUrls(List.of("https://cdn.test/" + i + ".png"))
                    .locked(false)
                    .build());
            canvasIds.add(c.getId());
            canvasNames.add(canvasName);
        }

        Address address = addressRepository.save(Address.builder()
                .userId(userId)
                .receiverName("张三")
                .phone("13800000000")
                .detail("测试地址 1 号")
                .build());
        addressId = address.getId();
    }

    // ===== 流程编排辅助 =====

    private Long createMainOrder() {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setSeriesId(seriesId);
        req.setCanvasIds(canvasIds);
        return orderService.createMainOrder(userId, req).getId();
    }

    private void review(Long orderId, boolean approved, String reason) {
        ReviewRequest req = new ReviewRequest();
        req.setResult(approved ? ReviewResult.APPROVED.name() : ReviewResult.REJECTED.name());
        req.setRejectReason(reason);
        orderService.reviewOrder(orderId, adminId, req);
    }

    private void quote(Long orderId, BigDecimal price) {
        QuoteRequest req = new QuoteRequest();
        req.setQuotedPrice(price);
        req.setExpectedDeliveryDate(LocalDate.now().plusDays(30).toString());
        orderService.quoteOrder(orderId, adminId, req);
    }

    /** 走到 DEPOSIT_PENDING */
    private Long createToDepositPending(BigDecimal price) {
        Long orderId = createMainOrder();
        review(orderId, true, null);
        quote(orderId, price);
        orderService.acceptQuote(orderId, userId);
        orderService.drawLottery(orderId, userId);
        orderService.bindAddress(orderId, userId, addressId);
        return orderId;
    }

    private void payDeposit(Long orderId) {
        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setChannel(PaymentChannel.WECHAT);
        PaymentResponse resp = paymentService.createDepositPayment(orderId, userId, req);
        paymentService.simulatePaymentSuccess(resp.getPaymentId(), userId);
    }

    private void payBalance(Long orderId) {
        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setChannel(PaymentChannel.ALIPAY);
        PaymentResponse resp = paymentService.createBalancePayment(orderId, userId, req);
        paymentService.simulatePaymentSuccess(resp.getPaymentId(), userId);
    }

    /** DEPOSIT_PENDING → ... → SHIPPED */
    private void runToShipped(Long orderId) {
        payDeposit(orderId);
        orderService.submitForQc(orderId, adminId);
        QualityCheckRequest qc = new QualityCheckRequest();
        qc.setResult(QcResult.PASSED.name());
        productionService.qualityCheck(orderId, adminId, qc);
        payBalance(orderId);
        ShipRequest ship = new ShipRequest();
        ship.setTrackingCompany("顺丰速运");
        ship.setTrackingNumber("SF" + System.currentTimeMillis());
        productionService.shipOrder(orderId, adminId, ship);
    }

    private OrderEntity reload(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow();
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual, "金额不应为 null");
        assertEquals(0, actual.compareTo(new BigDecimal(expected)),
                "金额不符,期望 " + expected + " 实际 " + actual);
    }

    // ===== 用例 =====

    @Test
    @DisplayName("主链路:下单到完成,状态机与金额全程正确")
    void mainFlowHappyPath() {
        Long orderId = createMainOrder();
        assertEquals(OrderStatus.REVIEWING, reload(orderId).getStatus());

        review(orderId, true, null);
        assertEquals(OrderStatus.QUOTED, reload(orderId).getStatus());

        quote(orderId, PRICE);
        assertMoney("1000.00", reload(orderId).getQuotedPrice());

        orderService.acceptQuote(orderId, userId);
        assertEquals(OrderStatus.LOTTERY_PENDING, reload(orderId).getStatus());

        orderService.drawLottery(orderId, userId);
        assertEquals(OrderStatus.LOTTERY_DONE, reload(orderId).getStatus());
        assertEquals(TIER.getSelectedCount(),
                orderCanvasRepository.findByOrderIdAndLotteryResult(orderId, LotteryResult.SELECTED).size());
        assertEquals(TIER.getNotSelectedCount(),
                orderCanvasRepository.findByOrderIdAndLotteryResult(orderId, LotteryResult.NOT_SELECTED).size());

        orderService.bindAddress(orderId, userId, addressId);
        OrderEntity afterBind = reload(orderId);
        assertEquals(OrderStatus.DEPOSIT_PENDING, afterBind.getStatus());
        assertMoney("500.00", afterBind.getDepositAmount());
        assertMoney("500.00", afterBind.getBalanceAmount());
        assertEquals(0, afterBind.getDepositAmount().add(afterBind.getBalanceAmount())
                .compareTo(afterBind.getQuotedPrice()), "定金 + 尾款 必须等于 报价");

        payDeposit(orderId);
        assertEquals(OrderStatus.IN_PRODUCTION, reload(orderId).getStatus());
        // 定金支付后所有画布锁定,未中签的也一样
        assertTrue(canvasRepository.findAllById(canvasIds).stream().allMatch(Canvas::getLocked),
                "定金支付后全部画布应被锁定");

        runToQcToBalance(orderId);
        assertEquals(OrderStatus.BALANCE_PENDING, reload(orderId).getStatus());

        payBalance(orderId);
        assertEquals(OrderStatus.SHIPPING_PENDING, reload(orderId).getStatus());

        ShipRequest ship = new ShipRequest();
        ship.setTrackingCompany("顺丰速运");
        ship.setTrackingNumber("SF1234567890");
        productionService.shipOrder(orderId, adminId, ship);
        OrderEntity shipped = reload(orderId);
        assertEquals(OrderStatus.SHIPPED, shipped.getStatus());
        assertEquals("SF1234567890", shipped.getTrackingNumber());
        // 发货开启 60 天补购窗口
        orderCanvasRepository.findByOrderIdAndLotteryResult(orderId, LotteryResult.NOT_SELECTED)
                .forEach(oc -> assertEquals(LocalDate.now().plusDays(60), oc.getRefillAvailableUntil()));
        orderCanvasRepository.findByOrderIdAndLotteryResult(orderId, LotteryResult.SELECTED)
                .forEach(oc -> assertNull(oc.getRefillAvailableUntil(), "中签角色不应有补购窗口"));

        productionService.confirmReceived(orderId, userId);
        assertEquals(OrderStatus.COMPLETED, reload(orderId).getStatus());

        // 审计链路:每一步状态转移都应落一条日志
        List<OrderStatusLog> logs = orderStatusLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        assertEquals(11, logs.size(), "主链路应产生 11 次状态转移");
        assertEquals(OrderStatus.DRAFT_SUBMIT_PENDING.name(), logs.get(0).getFromStatus());
        assertEquals(OrderStatus.COMPLETED.name(), logs.get(logs.size() - 1).getToStatus());
    }

    private void runToQcToBalance(Long orderId) {
        orderService.submitForQc(orderId, adminId);
        QualityCheckRequest qc = new QualityCheckRequest();
        qc.setResult(QcResult.PASSED.name());
        productionService.qualityCheck(orderId, adminId, qc);
    }

    @Test
    @DisplayName("画布名称:创建时落库,并在系列/订单/补购处一致回显")
    void canvasNamePersistsAndPropagates() {
        // 1. 服务创建:名字必须真的存下来,而不是像以前那样被静默丢弃
        CanvasCreateRequest req = new CanvasCreateRequest();
        req.setName("  星海旅人  ");
        Canvas created = canvasService.createCanvas(seriesId, userId, req);
        assertEquals("星海旅人", created.getName(), "创建时应保存并去掉首尾空格");
        assertEquals("星海旅人", canvasService.getCanvasDetail(created.getId(), userId).getName());

        // 2. 系列详情要回显名字(前端画布列表用它)
        assertTrue(seriesService.getSeriesDetail(seriesId, userId).getCanvases().stream()
                        .anyMatch(c -> "星海旅人".equals(c.getName())),
                "系列详情应回显画布名称");

        // 3. 订单详情必须用真实名字,不能再出现硬编码的 "画布 {id}"
        Long orderId = createToDepositPending(PRICE);
        List<String> namesInOrder = orderService.getOrderDetail(orderId, userId).getCanvases()
                .stream().map(OrderDetailResponse.CanvasInfo::getName).toList();
        assertTrue(namesInOrder.contains("测试角色 1"),
                "订单详情应使用画布名称,实际=" + namesInOrder);
        assertFalse(namesInOrder.stream().anyMatch(n -> n.startsWith("画布 ")),
                "不应再出现硬编码的 '画布 {id}',实际=" + namesInOrder);
    }

    @Test
    @DisplayName("画布名称:名称为空时兜底,不能写进 NOT NULL 列")
    void canvasNameBlankFallback() {
        CanvasCreateRequest req = new CanvasCreateRequest();
        req.setName("   ");
        Canvas created = canvasService.createCanvas(seriesId, userId, req);
        assertNotNull(created.getName());
        assertFalse(created.getName().isBlank(), "空名称必须兜底,不能写入空串");
    }

    @Test
    @DisplayName("金额拆分:奇数分必须保证 定金+尾款 == 报价")
    void moneySplitKeepsTotal() {
        Long orderId = createToDepositPending(new BigDecimal("1000.01"));
        OrderEntity order = reload(orderId);
        assertMoney("500.01", order.getDepositAmount());
        assertMoney("500.00", order.getBalanceAmount());
        assertEquals(0, order.getDepositAmount().add(order.getBalanceAmount())
                .compareTo(order.getQuotedPrice()));
    }

    @Test
    @DisplayName("返工:终审拒绝 → 修改后重新提交 → 再次进入终审")
    void reworkLoop() {
        Long orderId = createMainOrder();
        review(orderId, false, "第 3 个角色涉嫌侵权,请修改");

        assertEquals(OrderStatus.REVIEW_REJECTED, reload(orderId).getStatus());
        // 拒绝理由必须回传给用户,否则用户不知道要改什么
        assertEquals("第 3 个角色涉嫌侵权,请修改",
                orderService.getOrderDetail(orderId, userId).getRejectReason());
        assertEquals(1, reviewLogRepository.findByOrderIdOrderByReviewedAtDesc(orderId).size());

        orderService.resubmitOrder(orderId, userId);
        assertEquals(OrderStatus.DRAFT_SUBMIT_PENDING, reload(orderId).getStatus());

        // 重新提交后必须能再次进入终审队列,否则返工链路断死
        orderService.submitForReview(orderId, userId);
        assertEquals(OrderStatus.REVIEWING, reload(orderId).getStatus());

        review(orderId, true, null);
        quote(orderId, PRICE);
        assertEquals(OrderStatus.QUOTED, reload(orderId).getStatus());
    }

    @Test
    @DisplayName("返工:重新提交时画布不满足档位要求应被拒绝")
    void resubmitRejectsIncompleteDesign() {
        Long orderId = createMainOrder();
        review(orderId, false, "需要修改");
        orderService.resubmitOrder(orderId, userId);

        // 把其中一个画布退回设计中,模拟"改了一半就提交"
        Canvas c = canvasRepository.findById(canvasIds.get(0)).orElseThrow();
        c.setStatus(CanvasStatus.DESIGNING);
        canvasRepository.save(c);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.submitForReview(orderId, userId));
        assertEquals(ResultCode.CANVAS_NOT_FINALIZED.getCode(), ex.getCode());
        assertEquals(OrderStatus.DRAFT_SUBMIT_PENDING, reload(orderId).getStatus());
    }

    @Test
    @DisplayName("返工:非草稿态不能提交终审")
    void submitForReviewRejectsWrongStatus() {
        Long orderId = createMainOrder(); // REVIEWING
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.submitForReview(orderId, userId));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("关闭重开:拒绝报价 → 重开 → 再次提交终审")
    void closeAndReopen() {
        Long orderId = createMainOrder();
        review(orderId, true, null);
        quote(orderId, PRICE);

        orderService.rejectQuote(orderId, userId);
        assertEquals(OrderStatus.CLOSED, reload(orderId).getStatus());

        orderService.reopenOrder(orderId, userId);
        assertEquals(OrderStatus.DRAFT_SUBMIT_PENDING, reload(orderId).getStatus());

        orderService.submitForReview(orderId, userId);
        assertEquals(OrderStatus.REVIEWING, reload(orderId).getStatus());

        // 重复重开应被拒绝
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.reopenOrder(orderId, userId));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("报价前置检查:未填报价不能接受")
    void acceptQuoteRequiresPrice() {
        Long orderId = createMainOrder();
        review(orderId, true, null); // 终审通过即进入 QUOTED,此时价格还没填

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.acceptQuote(orderId, userId));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("报价尚未填写"), "实际:" + ex.getMessage());

        quote(orderId, PRICE);
        orderService.acceptQuote(orderId, userId);
        assertEquals(OrderStatus.LOTTERY_PENDING, reload(orderId).getStatus());
    }

    @Test
    @DisplayName("取消违约金:未付定金 → 零损失")
    void cancelBeforeDepositNoPenalty() {
        Long orderId = createToDepositPending(PRICE);

        CancelResponse resp = productionService.cancelOrder(orderId, userId);
        assertEquals(OrderStatus.CANCELLED, reload(orderId).getStatus());
        assertMoney("0.00", resp.getDepositPenaltyAmount());
        assertMoney("0.00", resp.getDepositRefundAmount());
        assertEquals(OrderStatus.DEPOSIT_PENDING.name(), resp.getStageAtCancel());
        assertFalse(cancellationLogRepository.findAllByOrderByCancelledAtDesc().isEmpty());
    }

    @Test
    @DisplayName("取消违约金:已付定金未开工 → 扣定金的 30%,退定金的 70%")
    void cancelAfterDepositPenalty30Percent() {
        Long orderId = createToDepositPending(PRICE);
        payDeposit(orderId);
        assertEquals(OrderStatus.IN_PRODUCTION, reload(orderId).getStatus());

        // 报价 1000 → 定金 500;规则是"扣定金的 30%",不是"扣总价的 30%"
        CancelResponse resp = productionService.cancelOrder(orderId, userId);
        assertMoney("150.00", resp.getDepositPenaltyAmount());
        assertMoney("350.00", resp.getDepositRefundAmount());
        assertEquals(OrderStatus.CANCELLED, reload(orderId).getStatus());
    }

    @Test
    @DisplayName("取消违约金:已开工 → 定金不退")
    void cancelAfterProductionStartedForfeitsDeposit() {
        Long orderId = createToDepositPending(PRICE);
        payDeposit(orderId);
        productionService.markProductionStarted(orderId, adminId);

        CancelResponse resp = productionService.cancelOrder(orderId, userId);
        assertMoney("500.00", resp.getDepositPenaltyAmount());
        assertMoney("0.00", resp.getDepositRefundAmount());
    }

    @Test
    @DisplayName("取消违约金:按实付金额计算,不受订单状态枚举顺序影响")
    void cancelPenaltyUsesActuallyPaidAmount() {
        // 构造"状态是 IN_PRODUCTION 但定金实付 1000"的场景(与订单字段 500 不一致),
        // 验证罚金按真实支付事实算,而不是按订单上的 depositAmount 字段算
        Long orderId = createToDepositPending(PRICE);
        payDeposit(orderId);
        Payment payment = paymentRepository.findByOrderIdAndType(orderId, PaymentType.DEPOSIT).get(0);
        payment.setAmount(new BigDecimal("1000.00"));
        paymentRepository.save(payment);

        CancelResponse resp = productionService.cancelOrder(orderId, userId);
        assertMoney("300.00", resp.getDepositPenaltyAmount());
        assertMoney("700.00", resp.getDepositRefundAmount());
    }

    @Test
    @DisplayName("补购:发货后可补购,定价 (总价÷中签数)×1.3,且不可重复")
    void refillHappyPath() {
        Long orderId = createToDepositPending(PRICE);
        runToShipped(orderId);

        List<RefillableCanvasResponse> refillable = refillService.getRefillableCanvases(orderId, userId);
        assertEquals(TIER.getNotSelectedCount(), refillable.size());
        refillable.forEach(r -> {
            assertFalse(r.getExpired(), "刚发货不应过期");
            assertMoney("325.00", r.getRefillPrice()); // 1000 / 4 * 1.3
            // 补购列表要用真实角色名,前端 RefillPage 直接渲染这个字段
            assertTrue(canvasNames.contains(r.getCanvasName()),
                    "补购列表应回显画布名称,实际=" + r.getCanvasName());
        });

        Long canvasId = refillable.get(0).getCanvasId();
        RefillRequest req = new RefillRequest();
        req.setCanvasId(canvasId);
        req.setChannel(PaymentChannel.WECHAT.name());
        RefillOrderResponse refill = refillService.createRefillOrder(orderId, userId, req);

        OrderEntity refillOrder = reload(refill.getRefillOrderId());
        assertEquals(OrderType.REFILL, refillOrder.getOrderType());
        assertEquals(orderId, refillOrder.getParentOrderId());
        assertEquals(OrderStatus.DEPOSIT_PENDING, refillOrder.getStatus(), "补购跳过终审/报价/抽奖");
        assertMoney("325.00", refillOrder.getQuotedPrice());
        assertMoney("162.50", refillOrder.getDepositAmount());
        assertMoney("162.50", refillOrder.getBalanceAmount());
        assertEquals(addressId, refillOrder.getAddressId(), "补购应复用主订单地址");

        // 补购订单也应能走完生产链路
        runToShipped(refill.getRefillOrderId());
        assertEquals(OrderStatus.SHIPPED, reload(refill.getRefillOrderId()).getStatus());

        // 重复补购同一角色应被拒绝
        BusinessException dup = assertThrows(BusinessException.class,
                () -> refillService.createRefillOrder(orderId, userId, req));
        assertEquals(ResultCode.CONFLICT.getCode(), dup.getCode());
    }

    @Test
    @DisplayName("补购:窗口过期后不可补购")
    void refillExpiredWindow() {
        Long orderId = createToDepositPending(PRICE);
        runToShipped(orderId);

        // 把补购窗口拨回昨天,模拟 60 天窗口已过
        orderCanvasRepository.findByOrderIdAndLotteryResult(orderId, LotteryResult.NOT_SELECTED)
                .forEach(oc -> {
                    oc.setRefillAvailableUntil(LocalDate.now().minusDays(1));
                    orderCanvasRepository.save(oc);
                });

        List<RefillableCanvasResponse> refillable = refillService.getRefillableCanvases(orderId, userId);
        assertFalse(refillable.isEmpty());
        assertTrue(refillable.stream().allMatch(RefillableCanvasResponse::getExpired),
                "过期后列表应标记为不可补购");

        RefillRequest req = new RefillRequest();
        req.setCanvasId(refillable.get(0).getCanvasId());
        req.setChannel(PaymentChannel.WECHAT.name());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> refillService.createRefillOrder(orderId, userId, req));
        assertEquals(ResultCode.REFILL_WINDOW_EXPIRED.getCode(), ex.getCode());

        // 过期扫描任务应能正常跑完且不报错
        assertDoesNotThrow(() -> refillExpiryScheduler.scanExpiredRefillWindows());
    }

    @Test
    @DisplayName("补购:主订单未发货时不可补购")
    void refillBeforeShipRejected() {
        Long orderId = createToDepositPending(PRICE);
        payDeposit(orderId);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refillService.getRefillableCanvases(orderId, userId));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("未发货"), "实际:" + ex.getMessage());
    }

    @Test
    @DisplayName("质检不通过:返工回生产中,可再次送检")
    void qcFailRework() {
        Long orderId = createToDepositPending(PRICE);
        payDeposit(orderId);
        orderService.submitForQc(orderId, adminId);
        assertEquals(OrderStatus.QC_PENDING, reload(orderId).getStatus());

        QualityCheckRequest fail = new QualityCheckRequest();
        fail.setResult(QcResult.FAILED.name());
        fail.setFailReason("涂装色差超标");
        productionService.qualityCheck(orderId, adminId, fail);
        assertEquals(OrderStatus.IN_PRODUCTION, reload(orderId).getStatus());

        orderService.submitForQc(orderId, adminId);
        QualityCheckRequest pass = new QualityCheckRequest();
        pass.setResult(QcResult.PASSED.name());
        productionService.qualityCheck(orderId, adminId, pass);
        assertEquals(OrderStatus.BALANCE_PENDING, reload(orderId).getStatus());
        assertEquals(2, productionService.getQcLogs(orderId).size(), "两次质检都应留档");
    }

    @Test
    @DisplayName("通知:状态流转产生站内消息,已读可清空未读")
    void notificationFlow() {
        Long orderId = createToDepositPending(PRICE);

        long unread = notificationService.countUnread(userId);
        assertTrue(unread >= 5, "到待付定金至少应产生 5 条站内消息,实际 " + unread);
        assertFalse(notificationService.listMyNotifications(userId).isEmpty());

        int marked = notificationService.markAllRead(userId);
        assertEquals(unread, marked);
        assertEquals(0, notificationService.countUnread(userId));

        // 不能替别人把消息标记为已读
        Notification mine = notificationService.listMyNotifications(userId).get(0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.markRead(mine.getId(), adminId));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("权限:他人不能操作自己的订单,管理员可查看")
    void ownershipAndAdminAccess() {
        Long orderId = createToDepositPending(PRICE);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.acceptQuote(orderId, adminId));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());

        // 管理员可以查看任意订单详情
        assertNotNull(orderService.getOrderDetail(orderId, adminId));

        // 模拟支付不能替别人刷单
        PaymentResponse resp = paymentService.createDepositPayment(orderId, userId,
                buildWechatRequest());
        BusinessException payEx = assertThrows(BusinessException.class,
                () -> paymentService.simulatePaymentSuccess(resp.getPaymentId(), adminId));
        assertEquals(ResultCode.FORBIDDEN.getCode(), payEx.getCode());
    }

    private PaymentCreateRequest buildWechatRequest() {
        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setChannel(PaymentChannel.WECHAT);
        return req;
    }

    @Test
    @DisplayName("删除保护:已产生订单的系列不可删除")
    void seriesWithOrderCannotBeDeleted() {
        createMainOrder();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> seriesService.deleteSeries(seriesId, userId));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());

        // 没有订单的系列照常可删
        Series empty = seriesRepository.save(Series.builder()
                .userId(userId).name("空系列").specTier(TIER)
                .sizeTier("STANDARD").designStatus(DesignStatus.DESIGNING).build());
        assertDoesNotThrow(() -> seriesService.deleteSeries(empty.getId(), userId));
    }

    @Test
    @DisplayName("终审是硬阻塞点:不允许绕过审核直接报价")
    void reviewIsHardBlock() {
        Long orderId = createMainOrder();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> quote(orderId, PRICE));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());

        // 拒绝态也不能直接报价
        review(orderId, false, "不合规");
        assertThrows(BusinessException.class, () -> quote(orderId, PRICE));
    }

    @Test
    @DisplayName("幂等:同一支付单重复回调不产生重复状态推进")
    void paymentCallbackIdempotent() {
        Long orderId = createToDepositPending(PRICE);
        PaymentResponse resp = paymentService.createDepositPayment(orderId, userId, buildWechatRequest());

        paymentService.simulatePaymentSuccess(resp.getPaymentId(), userId);
        long logsAfterFirst = orderStatusLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId).size();

        paymentService.simulatePaymentSuccess(resp.getPaymentId(), userId);
        assertEquals(logsAfterFirst,
                orderStatusLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId).size(),
                "重复回调不应再推进状态");
        assertEquals(1, paymentRepository.findByOrderId(orderId).size());
    }
}
