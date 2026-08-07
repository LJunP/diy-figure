package com.diyfigure.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 *
 * 支持用户名或邮箱登录(account 字段兼容两种)
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
