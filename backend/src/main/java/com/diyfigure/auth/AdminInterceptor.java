package com.diyfigure.auth;

import com.diyfigure.common.enums.UserRole;
import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.common.response.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 运营端(管理员)授权拦截器
 *
 * 背景:JwtInterceptor 只校验"是否登录",不校验"是什么角色"。
 * 前端路由守卫只能隐藏入口,不能阻止直接调用 API,因此必须在服务端再校验一次。
 *
 * 拦截所有 /{module}/admin/** 路径(如 /orders/admin/**、/production/admin/**),
 * 要求当前登录用户角色为 ADMIN,否则返回 403。
 *
 * 注册顺序必须在 JwtInterceptor 之后,才能读到 CURRENT_USER_ROLE 属性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String role = JwtInterceptor.getCurrentUserRole(request);
        if (!UserRole.ADMIN.name().equals(role)) {
            Long userId = JwtInterceptor.getCurrentUserId(request);
            log.warn("越权访问运营接口被拦截: userId={}, role={}, uri={}", userId, role, request.getRequestURI());
            return writeForbidden(response);
        }

        return true;
    }

    /**
     * 返回 403 无权限响应
     */
    private boolean writeForbidden(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> apiResponse = ApiResponse.error(ResultCode.FORBIDDEN, "需要运营管理员权限");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        return false;
    }
}
