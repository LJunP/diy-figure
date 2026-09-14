package com.diyfigure.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class TokenRequest {

    private TokenRequest() {
    }

    @Data
    public static class EmailOnly {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
    }

    @Data
    public static class Verify {
        @NotBlank(message = "验证令牌不能为空")
        private String token;
    }

    @Data
    public static class ResetPassword {
        @NotBlank(message = "重置令牌不能为空")
        private String token;

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, message = "密码至少 6 位")
        private String newPassword;
    }
}
