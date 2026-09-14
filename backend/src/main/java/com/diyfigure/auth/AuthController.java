package com.diyfigure.auth;

import com.diyfigure.auth.dto.AuthResponse;
import com.diyfigure.auth.dto.LoginRequest;
import com.diyfigure.auth.dto.RegisterRequest;
import com.diyfigure.auth.dto.TokenRequest;
import com.diyfigure.auth.dto.UpdatePasswordRequest;
import com.diyfigure.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * 公开接口(JWT 白名单):
 * - POST /api/auth/register
 * - POST /api/auth/login
 * - POST /api/auth/verify-email
 * - POST /api/auth/resend-verification
 * - POST /api/auth/forgot-password
 * - POST /api/auth/reset-password
 *
 * 需登录:
 * - GET  /api/auth/me
 * - PUT  /api/auth/password
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody TokenRequest.Verify body) {
        authService.verifyEmail(body.getToken());
        return ApiResponse.success();
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@Valid @RequestBody TokenRequest.EmailOnly body) {
        authService.resendVerification(body.getEmail());
        return ApiResponse.success();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody TokenRequest.EmailOnly body) {
        authService.forgotPassword(body.getEmail());
        return ApiResponse.success();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody TokenRequest.ResetPassword body) {
        authService.resetPassword(body.getToken(), body.getNewPassword());
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<AuthResponse.UserInfo> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(authService.getUserInfo(userId));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(HttpServletRequest request,
                                            @Valid @RequestBody UpdatePasswordRequest body) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        authService.updatePassword(userId, body);
        return ApiResponse.success();
    }
}
