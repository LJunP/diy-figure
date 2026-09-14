package com.diyfigure.common.controller;

import com.diyfigure.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查。JWT 白名单。
 * 数据库不通时 status=DOWN,HTTP 仍 200(探活进程还活着),由编排按 data.status 判断。
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "diy-figure-backend");
        data.put("timestamp", LocalDateTime.now().toString());
        try (Connection ignored = dataSource.getConnection()) {
            data.put("status", "UP");
            data.put("database", "UP");
        } catch (Exception e) {
            data.put("status", "DOWN");
            data.put("database", "DOWN");
        }
        return ApiResponse.success(data);
    }
}
