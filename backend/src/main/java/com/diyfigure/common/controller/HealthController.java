package com.diyfigure.common.controller;

import com.diyfigure.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查接口
 * 用于验证后端服务是否正常运行
 * 路径:/api/health(在 JWT 拦截器白名单中)
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "service", "diy-figure-backend",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
