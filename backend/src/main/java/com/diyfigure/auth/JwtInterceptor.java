package com.diyfigure.auth;

import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.config.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器
 * 拦截所有需要认证的请求,校验 JWT token
 *
 * 校验通过后,将 userId 和 role 存入 request attribute,供 Controller 使用
 * 校验失败时,返回 401 未授权响应
 *
 * 注意:不使用 Spring Security,采用自定义拦截器实现鉴权(按技术栈确认)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    public static final String CURRENT_USER_ID = "currentUserId";
    public static final String CURRENT_USER_ROLE = "currentUserRole";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从请求头获取 token
        String authHeader = request.getHeader(jwtProperties.getHeader());
        if (authHeader == null || !authHeader.startsWith(jwtProperties.getPrefix())) {
            return writeUnauthorized(response, "缺少认证信息");
        }

        String token = authHeader.substring(jwtProperties.getPrefix().length()).trim();

        // 校验 token
        if (!jwtUtil.validateToken(token)) {
            return writeUnauthorized(response, "token 已过期或无效");
        }

        // 提取用户信息,存入 request attribute
        Claims claims = jwtUtil.parseToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);

        request.setAttribute(CURRENT_USER_ID, userId);
        request.setAttribute(CURRENT_USER_ROLE, role);

        return true;
    }

    /**
     * 返回 401 未授权响应
     */
    private boolean writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> apiResponse = ApiResponse.error(ResultCode.UNAUTHORIZED, message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        return false;
    }

    /**
     * 从 request 中获取当前登录用户 ID(供 Controller/Service 使用)
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CURRENT_USER_ID);
    }

    /**
     * 从 request 中获取当前登录用户角色
     */
    public static String getCurrentUserRole(HttpServletRequest request) {
        return (String) request.getAttribute(CURRENT_USER_ROLE);
    }
}
