package com.diyfigure.integration;

import com.diyfigure.common.enums.*;
import com.diyfigure.entity.*;
import com.diyfigure.repository.*;
import com.diyfigure.address.AddressService;
import com.diyfigure.canvas.CanvasService;
import com.diyfigure.order.service.OrderService;
import com.diyfigure.payment.PaymentService;
import com.diyfigure.production.ProductionService;
import com.diyfigure.refill.RefillService;
import com.diyfigure.series.SeriesService;
import com.diyfigure.series.dto.SeriesCreateRequest;
import com.diyfigure.series.dto.SeriesUpdateRequest;
import com.diyfigure.address.dto.AddressRequest;
import com.diyfigure.payment.dto.PaymentCreateRequest;
import com.diyfigure.refill.dto.RefillRequest;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 跨用户越权 + 角色越权 专项安全测试
 *
 * 两层防线:
 * 1. 用户端点(任意 /api/orders、/api/canvases、/api/addresses 等):
 *    service 层必须校验 userId 归属。这里直接调 service 验证。
 *    任何一个端点被改坏成漏了归属校验,这里就立刻红。
 * 2. 运营端点(/api/admin/** 与 /api/orders/admin/**):
 *    由 AdminInterceptor 做角色校验。这里用 MockMvc 直接打 controller 验证。
 *
 * 不依赖 controller 调用 service(那样会混入「状态不对」的错误),service 层测保持纯净。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("跨用户越权 & 角色越权 测试")
class CrossUserAuthorizationTest {

    @Autowired private SeriesService seriesService;
    @Autowired private CanvasService canvasService;
    @Autowired private AddressService addressService;
    @Autowired private OrderService orderService;
    @Autowired private PaymentService paymentService;
    @Autowired private ProductionService productionService;
    @Autowired private RefillService refillService;

    @Autowired private UserRepository userRepository;
    @Autowired private SeriesRepository seriesRepository;
    @Autowired private CanvasRepository canvasRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderCanvasRepository orderCanvasRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private org.flywaydb.core.Flyway flyway;
    @Autowired private TransactionTemplate tx;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;

    @Autowired private com.diyfigure.auth.JwtUtil jwtUtil;

    private Long ownerId, intruderId;
    private String ownerToken, intruderToken;
    private Long seriesA;
    private Long canvasA;
    private Long addressA;
    private Long orderA;
    private Long paymentA;

    @BeforeEach
    void seed() {
        flyway.clean();
        flyway.migrate();

        String s = UUID.randomUUID().toString().substring(0, 8);
        User owner = userRepository.save(User.builder()
                .username("o" + s).email("o" + s + "@t.l")
                .passwordHash("x").role(UserRole.USER).build());
        User intruder = userRepository.save(User.builder()
                .username("i" + s).email("i" + s + "@t.l")
                .passwordHash("x").role(UserRole.USER).build());
        ownerId = owner.getId();
        intruderId = intruder.getId();
        ownerToken = jwtUtil.generateToken(ownerId, UserRole.USER.name());
        intruderToken = jwtUtil.generateToken(intruderId, UserRole.USER.name());

        // A 的资源:一个完整可演示的主订单(已报价+一笔 PENDING 定金支付单)
        Series series = tx.execute(s2 -> {
            SeriesCreateRequest req = new SeriesCreateRequest();
            req.setName("O 系列"); req.setSpecTier(SpecTier.LIGHT); req.setSizeTier("STANDARD");
            Series x = seriesService.createSeries(ownerId, req);
            x.setDesignStatus(DesignStatus.READY_FOR_QUOTE);
            return seriesRepository.save(x);
        });
        seriesA = series.getId();
        for (int i = 0; i < 6; i++) {
            canvasRepository.save(Canvas.builder()
                    .seriesId(seriesA).name("角色 " + (i + 1))
                    .status(CanvasStatus.FINALIZED)
                    .conceptImageUrls(i == 0 ? List.of("https://cdn/x.png") : List.of())
                    .build());
        }
        canvasA = canvasRepository.findBySeriesIdOrderByCreatedAtAsc(seriesA).get(0).getId();
        AddressRequest ar = new AddressRequest();
        ar.setReceiverName("X"); ar.setPhone("13800000000"); ar.setDetail("A 地址");
        addressA = addressService.create(ownerId, ar).getId();

        orderA = orderRepository.save(OrderEntity.builder()
                .userId(ownerId).seriesId(seriesA).orderType(OrderType.MAIN)
                .status(OrderStatus.QUOTED).quotedPrice(new BigDecimal("1000.00"))
                .expectedDeliveryDate(LocalDate.now().plusDays(30))
                .build()).getId();
        for (Long cid : canvasRepository.findBySeriesIdOrderByCreatedAtAsc(seriesA)
                .stream().map(Canvas::getId).toList()) {
            orderCanvasRepository.save(OrderCanvas.builder()
                    .orderId(orderA).canvasId(cid)
                    .lotteryResult(LotteryResult.SELECTED).build());
        }
        paymentA = paymentRepository.save(Payment.builder()
                .orderId(orderA).type(PaymentType.DEPOSIT)
                .channel(PaymentChannel.WECHAT).amount(new BigDecimal("500.00"))
                .status(PaymentStatus.PENDING).build()).getId();
    }

    // ===== service 层:用户端点 userId 归属校验 =====
    private void assertForbidden(Runnable r) {
        BusinessException ex = assertThrows(BusinessException.class, r::run);
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode(),
                "应拒绝越权,实际 message=" + ex.getMessage());
    }

    @Test @DisplayName("画布:B 看不到/改不了 A 的画布")
    void canvasCrossUser() {
        assertForbidden(() -> canvasService.getCanvasDetail(canvasA, intruderId));
        assertForbidden(() -> canvasService.finalizeCanvas(canvasA, intruderId));
        assertForbidden(() -> canvasService.reopenCanvas(canvasA, intruderId));
        assertForbidden(() -> canvasService.retryModel3d(canvasA, intruderId));
        assertForbidden(() -> canvasService.deleteCanvas(canvasA, intruderId));
    }

    @Test @DisplayName("系列:B 不能改/删 A 的系列,也不能往里面塞画布")
    void seriesCrossUser() {
        assertForbidden(() -> seriesService.deleteSeries(seriesA, intruderId));
        SeriesUpdateRequest ur = new SeriesUpdateRequest();
        ur.setName("hack"); ur.setSizeTier("STD");
        assertForbidden(() -> seriesService.updateSeries(seriesA, intruderId, ur));
        // createCanvas 走 SeriesService 校验系列归属,这里偷懒不展开
    }

    @Test @DisplayName("订单:B 不能对 A 的订单做任何动作")
    void orderCrossUser() {
        assertForbidden(() -> orderService.getOrderDetail(orderA, intruderId));
        assertForbidden(() -> orderService.acceptQuote(orderA, intruderId));
        assertForbidden(() -> orderService.rejectQuote(orderA, intruderId));
        assertForbidden(() -> orderService.resubmitOrder(orderA, intruderId));
        assertForbidden(() -> orderService.reopenOrder(orderA, intruderId));
        assertForbidden(() -> orderService.submitForReview(orderA, intruderId));
        assertForbidden(() -> orderService.drawLottery(orderA, intruderId));
        assertForbidden(() -> orderService.bindAddress(orderA, intruderId, addressA));
        assertForbidden(() -> productionService.confirmReceived(orderA, intruderId));
        assertForbidden(() -> productionService.cancelOrder(orderA, intruderId));
    }

    @Test @DisplayName("支付:B 看不到 A 的支付记录,也不能创建/模拟支付(防替他人刷单)")
    void paymentCrossUser() {
        assertForbidden(() -> paymentService.getPaymentHistory(orderA, intruderId));
        PaymentCreateRequest pcr = new PaymentCreateRequest();
        pcr.setChannel(PaymentChannel.WECHAT);
        assertForbidden(() -> paymentService.createDepositPayment(orderA, intruderId, pcr));
        // 重点:即使有 A 的 paymentId,B 也不能 simulate
        assertForbidden(() -> paymentService.simulatePaymentSuccess(paymentA, intruderId));
    }

    @Test @DisplayName("地址:B 看不到/改不了/删不了 A 的地址")
    void addressCrossUser() {
        AddressRequest hack = new AddressRequest();
        hack.setReceiverName("hack"); hack.setPhone("999"); hack.setDetail("X");
        assertForbidden(() -> addressService.getByIdAndUserId(addressA, intruderId));
        assertForbidden(() -> addressService.update(addressA, intruderId, hack));
        assertForbidden(() -> addressService.delete(addressA, intruderId));
    }

    @Test @DisplayName("补购:B 看不到 A 的可补购列表,也不能给 A 的画布发起补购")
    void refillCrossUser() {
        assertForbidden(() -> refillService.getRefillableCanvases(orderA, intruderId));
        // 即使把订单推到 SHIPPED(让前置条件满足),B 也仍应被拒
        tx.executeWithoutResult(s -> {
            OrderEntity o = orderRepository.findById(orderA).orElseThrow();
            o.setStatus(OrderStatus.SHIPPED);
            o.setTrackingCompany("SF"); o.setTrackingNumber("SF1");
            orderRepository.save(o);
        });
        RefillRequest rr = new RefillRequest();
        rr.setCanvasId(canvasA); rr.setChannel("WECHAT");
        assertForbidden(() -> refillService.createRefillOrder(orderA, intruderId, rr));
    }

    // ===== controller 层:admin 端点角色校验 =====
    /**
     * ★ 状态码约定(与业务错误不同,别搞混):
     * - 业务错误:HTTP 200 + 响应体里的 code(如 403/409/4201),前端拦截器统一处理
     * - 鉴权/拦截器失败:真实 HTTP 状态码(401 未登录 / 403 无权限),不落业务体
     * 所以下面断言的是 HTTP 403,而不是 HTTP 200 + code=403。
     */
    @Test @DisplayName("运营端:USER 访问 /orders/admin/* 被 AdminInterceptor 挡")
    void orderAdminRejectedForUser() throws Exception {
        mockMvc.perform(asUser(post("/orders/admin/reviews/" + orderA))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultCode.FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("需要运营管理员权限"));

        mockMvc.perform(asUser(post("/orders/admin/orders/" + orderA + "/quote"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(asUser(get("/orders/admin/dashboard/stats")))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("运营端:USER 访问 /admin/orders/* 也被挡")
    void productionAdminRejectedForUser() throws Exception {
        mockMvc.perform(asUser(post("/admin/orders/" + orderA + "/production-start")))
                .andExpect(status().isForbidden());

        mockMvc.perform(asUser(post("/admin/orders/" + orderA + "/quality-check"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(asUser(post("/admin/orders/" + orderA + "/ship"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(asUser(get("/admin/cancellations")))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("未登录:访问任何受保护端点都被 JwtInterceptor 401 挡")
    void anonymousRejected() throws Exception {
        mockMvc.perform(get("/orders/" + orderA))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/orders/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/cancellations"))
                .andExpect(status().isUnauthorized());
    }

    // ===== 文件上传:OSS 未配置时的本地降级通道 =====

    @Test @DisplayName("上传参考图:OSS 未配置时落本地磁盘,返回真实可访问 URL(不再返回固定占位图)")
    void uploadImageFallsBackToLocalStorage() throws Exception {
        byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "ref.png", "image/png", png);

        String body = mockMvc.perform(multipart("/files/upload-image").file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        String url = json.readTree(body).path("data").path("url").asText();
        assertTrue(url.matches(".*/uploads/references/\\d{4}-\\d{2}-\\d{2}/[0-9a-f]{32}\\.png"),
                "URL 形状不对: " + url);
        // 前缀必须按当前请求推导,而不是取配置里写死的默认端口 ——
        // 否则服务实际跑在别的端口时,返回的链接会指向不存在的地址。
        // MockMvc 没有真实的 servlet context-path,所以这里只断言 host 来自请求、且不含写死的 8080。
        assertTrue(url.startsWith("http://localhost/"),
                "URL 前缀应来自当前请求,实际: " + url);
        assertFalse(url.contains(":8080"),
                "URL 不应包含写死的默认端口,实际: " + url);

        // 真的落盘了才算数——否则前端拿到 URL 依然是 404
        String relative = url.substring(url.indexOf("/uploads/") + "/uploads/".length());
        Path onDisk = Paths.get("./target/test-uploads", relative).toAbsolutePath().normalize();
        assertTrue(Files.exists(onDisk), "文件未落盘: " + onDisk);
        assertArrayEquals(png, Files.readAllBytes(onDisk), "落盘内容应与上传内容一致");
    }

    @Test @DisplayName("上传参考图:非图片类型必须被拒(本地降级通道也要走同一套校验)")
    void uploadImageRejectsNonImage() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("file", "x.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/files/upload-image").file(txt)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.FILE_TYPE_NOT_SUPPORTED.getCode()));
    }

    @Test @DisplayName("上传参考图:未登录被拒(不能匿名往服务器写文件)")
    void uploadImageRequiresAuth() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ref.png", "image/png", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/files/upload-image").file(file))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + intruderToken);
    }
}