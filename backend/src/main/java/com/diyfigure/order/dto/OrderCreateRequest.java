package com.diyfigure.order.dto;

import com.diyfigure.common.enums.OrderType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求 DTO
 *
 * 用户选择系列和画布后,创建主订单
 * Phase 4 仅支持主订单创建,补购订单在 Phase 7 实现
 */
@Data
public class OrderCreateRequest {

    @NotNull(message = "系列 ID 不能为空")
    private Long seriesId;

    /**
     * 选择的画布 ID 列表
     * 创建主订单时,必须包含该系列下所有已定稿的画布
     * 前端可让用户"全选"已定稿画布,后端校验是否达到档位要求
     */
    @NotNull(message = "画布列表不能为空")
    @Size(min = 1, message = "至少选择 1 个画布")
    private List<Long> canvasIds;
}
