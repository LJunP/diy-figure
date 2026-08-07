package com.diyfigure.production;

import com.diyfigure.auth.JwtInterceptor;
import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.entity.CancellationLog;
import com.diyfigure.entity.QualityCheckLog;
import com.diyfigure.production.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 生产与物流控制器
 *
 * 运营端接口:
 * - POST /api/admin/orders/{orderId}/production-start  标记生产开工
 * - POST /api/admin/orders/{orderId}/quality-check     质检结果录入
 * - POST /api/admin/orders/{orderId}/ship              发货(录入物流单号)
 * - POST /api/admin/orders/{orderId}/complete          标记订单完成
 * - GET  /api/admin/cancellations                       违约记录列表
 * - GET  /api/admin/orders/{orderId}/qc-logs            质检记录
 *
 * 用户端接口:
 * - POST /api/orders/{orderId}/confirm-received         确认签收
 * - POST /api/orders/{orderId}/cancel                   取消订单(含违约金计算)
 */
@RestController
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;

    // ===== 运营端接口 =====

    /**
     * 标记生产实际开工
     */
    @PostMapping("/admin/orders/{orderId}/production-start")
    public ApiResponse<Void> markProductionStarted(@PathVariable Long orderId,
                                                    HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        productionService.markProductionStarted(orderId, adminId);
        return ApiResponse.success();
    }

    /**
     * 质检结果录入
     */
    @PostMapping("/admin/orders/{orderId}/quality-check")
    public ApiResponse<Void> qualityCheck(@PathVariable Long orderId,
                                          @Valid @RequestBody QualityCheckRequest body,
                                          HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        productionService.qualityCheck(orderId, adminId, body);
        return ApiResponse.success();
    }

    /**
     * 发货(录入物流单号)
     */
    @PostMapping("/admin/orders/{orderId}/ship")
    public ApiResponse<Void> shipOrder(@PathVariable Long orderId,
                                       @Valid @RequestBody ShipRequest body,
                                       HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        productionService.shipOrder(orderId, adminId, body);
        return ApiResponse.success();
    }

    /**
     * 标记订单完成
     */
    @PostMapping("/admin/orders/{orderId}/complete")
    public ApiResponse<Void> completeOrder(@PathVariable Long orderId,
                                           HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        productionService.completeOrder(orderId, adminId);
        return ApiResponse.success();
    }

    /**
     * 违约记录列表(运营端)
     */
    @GetMapping("/admin/cancellations")
    public ApiResponse<List<CancellationLog>> listCancellations() {
        return ApiResponse.success(productionService.listCancellations());
    }

    /**
     * 质检记录(运营端)
     */
    @GetMapping("/admin/orders/{orderId}/qc-logs")
    public ApiResponse<List<QualityCheckLog>> getQcLogs(@PathVariable Long orderId) {
        return ApiResponse.success(productionService.getQcLogs(orderId));
    }

    // ===== 用户端接口 =====

    /**
     * 确认签收
     */
    @PostMapping("/orders/{orderId}/confirm-received")
    public ApiResponse<Void> confirmReceived(@PathVariable Long orderId,
                                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        productionService.confirmReceived(orderId, userId);
        return ApiResponse.success();
    }

    /**
     * 取消订单(含违约金计算)
     */
    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<CancelResponse> cancelOrder(@PathVariable Long orderId,
                                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(productionService.cancelOrder(orderId, userId));
    }
}
