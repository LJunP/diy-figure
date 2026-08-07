package com.diyfigure.production.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发货请求 DTO
 */
@Data
public class ShipRequest {

    @NotBlank(message = "物流公司不能为空")
    private String trackingCompany;

    @NotBlank(message = "物流单号不能为空")
    private String trackingNumber;
}
