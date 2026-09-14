package com.diyfigure.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BindAddressRequest {

    @NotNull(message = "请选择收货地址")
    private Long addressId;
}
