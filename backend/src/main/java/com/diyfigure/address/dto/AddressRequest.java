package com.diyfigure.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址请求 DTO
 */
@Data
public class AddressRequest {

    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 50, message = "姓名最多 50 个字符")
    private String receiverName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "收货地址不能为空")
    @Size(max = 500, message = "地址最多 500 个字符")
    private String detail;
}
