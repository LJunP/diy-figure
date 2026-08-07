package com.diyfigure.order;

import com.diyfigure.auth.JwtInterceptor;
import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.entity.OrderEntity;
import com.diyfigure.order.dto.*;
import com.diyfigure.order.service.OrderService;
import com.diyfigure.repository.OrderRepository;
import com.diyfigure.repository.ReviewLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 *
 * 用户端接口:
 * - POST /api/orders 创建主订单
 * - GET /api/orders 查询订单列表
 * - GET /api/orders/{id} 订单详情
 * - POST /api/orders/{id}/accept-quote 接受报价
 * - POST /api/orders/{id}/reject-quote 拒绝报价
 * - POST /api/orders/{id}/lottery/draw 执行抽奖
 *
 * 运营端接口(Phase 4):
 * - GET /api/admin/reviews/pending 待终审列表
 * - POST /api/admin/reviews/{orderId} 终审操作
 * - POST /api/admin/orders/{orderId}/quote 报价
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ReviewLogRepository reviewLogRepository;

    /**
     * 创建主订单
     */
    @PostMapping
    public ApiResponse<OrderDetailResponse> createOrder(@Valid @RequestBody OrderCreateRequest body,
                                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(orderService.createMainOrder(userId, body));
    }

    /**
     * 查询当前用户的所有主订单
     */
    @GetMapping
    public ApiResponse<List<OrderEntity>> listOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(orderService.listMainOrders(userId));
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderDetailResponse> getOrderDetail(@PathVariable Long id,
                                                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(orderService.getOrderDetail(id, userId));
    }

    /**
     * 接受报价
     */
    @PostMapping("/{id}/accept-quote")
    public ApiResponse<Void> acceptQuote(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        orderService.acceptQuote(id, userId);
        return ApiResponse.success();
    }

    /**
     * 拒绝报价
     */
    @PostMapping("/{id}/reject-quote")
    public ApiResponse<Void> rejectQuote(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        orderService.rejectQuote(id, userId);
        return ApiResponse.success();
    }

    /**
     * 执行抽奖
     */
    @PostMapping("/{id}/lottery/draw")
    public ApiResponse<Map<String, List<com.diyfigure.entity.Canvas>>> drawLottery(@PathVariable Long id,
                                                                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(orderService.drawLottery(id, userId));
    }

    /**
     * 绑定收货地址(抽奖完成后填写)
     */
    @PostMapping("/{id}/address")
    public ApiResponse<Void> bindAddress(@PathVariable Long id,
                                         @RequestBody java.util.Map<String, Long> body,
                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        orderService.bindAddress(id, userId, body.get("addressId"));
        return ApiResponse.success();
    }

    // ===== 运营端接口 =====

    /**
     * 待终审列表(运营端)
     */
    @GetMapping("/admin/reviews/pending")
    public ApiResponse<List<OrderEntity>> listPendingReviews() {
        return ApiResponse.success(orderRepository.findByStatus(
                com.diyfigure.common.enums.OrderStatus.REVIEWING
        ));
    }

    /**
     * 待报价列表(运营端)
     * 返回终审通过后进入 QUOTED 状态但尚未填写报价金额的订单
     */
    @GetMapping("/admin/quotes/pending")
    public ApiResponse<List<OrderEntity>> listPendingQuotes() {
        return ApiResponse.success(orderRepository.findByStatus(
                com.diyfigure.common.enums.OrderStatus.QUOTED
        ));
    }

    /**
     * 运营看板统计数据(优化:使用 countByStatus 避免 N+1 全表加载)
     */
    @GetMapping("/admin/dashboard/stats")
    public ApiResponse<java.util.Map<String, Long>> getDashboardStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("pendingReviews", orderRepository.countByStatus(
                com.diyfigure.common.enums.OrderStatus.REVIEWING));
        stats.put("pendingQuotes", orderRepository.countByStatus(
                com.diyfigure.common.enums.OrderStatus.QUOTED));
        stats.put("inProduction", orderRepository.countByStatus(
                com.diyfigure.common.enums.OrderStatus.IN_PRODUCTION));
        stats.put("pendingShipping", orderRepository.countByStatus(
                com.diyfigure.common.enums.OrderStatus.SHIPPING_PENDING));
        stats.put("pendingQc", orderRepository.countByStatus(
                com.diyfigure.common.enums.OrderStatus.QC_PENDING));
        stats.put("shipped", orderRepository.countByStatus(
                com.diyfigure.common.enums.OrderStatus.SHIPPED));
        stats.put("completed", orderRepository.countByStatus(
                com.diyfigure.common.enums.OrderStatus.COMPLETED));
        stats.put("depositPending", orderRepository.countByStatus(
                com.diyfigure.common.enums.OrderStatus.DEPOSIT_PENDING));
        stats.put("balancePending", orderRepository.countByStatus(
                com.diyfigure.common.enums.OrderStatus.BALANCE_PENDING));
        return ApiResponse.success(stats);
    }

    /**
     * 终审操作(运营端)
     */
    @PostMapping("/admin/reviews/{orderId}")
    public ApiResponse<Void> reviewOrder(@PathVariable Long orderId,
                                        @Valid @RequestBody ReviewRequest body,
                                        HttpServletRequest request) {
        Long reviewerId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        orderService.reviewOrder(orderId, reviewerId, body);
        return ApiResponse.success();
    }

    /**
     * 报价(运营端)
     */
    @PostMapping("/admin/orders/{orderId}/quote")
    public ApiResponse<Void> quoteOrder(@PathVariable Long orderId,
                                       @Valid @RequestBody QuoteRequest body,
                                       HttpServletRequest request) {
        Long reviewerId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        orderService.quoteOrder(orderId, reviewerId, body);
        return ApiResponse.success();
    }

    /**
     * 送检(运营端): IN_PRODUCTION → QC_PENDING
     */
    @PostMapping("/admin/orders/{orderId}/submit-qc")
    public ApiResponse<Void> submitForQc(@PathVariable Long orderId,
                                         HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        orderService.submitForQc(orderId, adminId);
        return ApiResponse.success();
    }

    /**
     * 待发货列表(运营端)
     */
    @GetMapping("/admin/shipping/pending")
    public ApiResponse<List<OrderEntity>> listPendingShipping() {
        return ApiResponse.success(orderRepository.findByStatus(
                com.diyfigure.common.enums.OrderStatus.SHIPPING_PENDING
        ));
    }

    /**
     * 生产中订单列表(运营端)
     */
    @GetMapping("/admin/production/list")
    public ApiResponse<List<OrderEntity>> listInProduction() {
        return ApiResponse.success(orderRepository.findByStatusOrderByCreatedAtDesc(
                com.diyfigure.common.enums.OrderStatus.IN_PRODUCTION
        ));
    }

    /**
     * 待质检列表(运营端)
     */
    @GetMapping("/admin/qc/pending")
    public ApiResponse<List<OrderEntity>> listPendingQc() {
        return ApiResponse.success(orderRepository.findByStatusOrderByCreatedAtDesc(
                com.diyfigure.common.enums.OrderStatus.QC_PENDING
        ));
    }

    /**
     * 已发货列表(运营端)
     */
    @GetMapping("/admin/shipped/list")
    public ApiResponse<List<OrderEntity>> listShipped() {
        return ApiResponse.success(orderRepository.findByStatusOrderByCreatedAtDesc(
                com.diyfigure.common.enums.OrderStatus.SHIPPED
        ));
    }
}
