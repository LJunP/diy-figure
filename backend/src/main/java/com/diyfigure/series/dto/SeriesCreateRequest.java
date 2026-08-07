package com.diyfigure.series.dto;

import com.diyfigure.common.enums.SpecTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建系列请求 DTO
 */
@Data
public class SeriesCreateRequest {

    @NotBlank(message = "系列名称不能为空")
    @Size(max = 100, message = "名称最多 100 个字符")
    private String name;

    @NotNull(message = "规格档位不能为空")
    private SpecTier specTier;

    @NotBlank(message = "尺寸档位不能为空")
    private String sizeTier;
}
