package com.diyfigure.auth;

import com.diyfigure.auth.dto.AuthResponse;
import com.diyfigure.auth.dto.LoginRequest;
import com.diyfigure.auth.dto.RegisterRequest;
import com.diyfigure.auth.dto.UpdatePasswordRequest;
import com.diyfigure.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * 接口清单(对应 03-技术设计说明书.md 第 7.1 节):
 * - POST /api/auth/register  注册
 * - POST /api/auth/login     登录,返回 JWT
 * - GET  /api/auth/me        获取当前用户信息
 * - PUT  /api/auth/password  修改密码
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 获取当前登录用户信息
     * 需要携带 token
     */
    @GetMapping("/me")
    public ApiResponse<AuthResponse.UserInfo> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(authService.getUserInfo(userId));
    }

    /**
     * 修改密码
     * 需要携带 token
     */
    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(HttpServletRequest request,
                                            @Valid @RequestBody UpdatePasswordRequest body) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        authService.updatePassword(userId, body);
        return ApiResponse.success();
    }
}
