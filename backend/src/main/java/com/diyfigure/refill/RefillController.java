package com.diyfigure.refill;

import com.diyfigure.auth.JwtInterceptor;
import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.entity.OrderEntity;
import com.diyfigure.refill.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 补购控制器
 *
 * 用户端接口:
 * - GET  /api/orders/{orderId}/refill/available  查看可补购角色列表
 * - POST /api/orders/{orderId}/refill             发起补购
 * - GET  /api/refills                             查看我的补购订单
 */
@RestController
@RequiredArgsConstructor
public class RefillController {

    private final RefillService refillService;

    /**
     * 查看可补购角色列表
     */
    @GetMapping("/orders/{orderId}/refill/available")
    public ApiResponse<List<RefillableCanvasResponse>> getRefillableCanvases(
            @PathVariable Long orderId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(refillService.getRefillableCanvases(orderId, userId));
    }

    /**
     * 发起补购
     */
    @PostMapping("/orders/{orderId}/refill")
    public ApiResponse<RefillOrderResponse> createRefillOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody RefillRequest body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(refillService.createRefillOrder(orderId, userId, body));
    }

    /**
     * 查看我的补购订单列表
     */
    @GetMapping("/refills")
    public ApiResponse<List<OrderEntity>> listRefillOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(refillService.listRefillOrders(userId));
    }
}
