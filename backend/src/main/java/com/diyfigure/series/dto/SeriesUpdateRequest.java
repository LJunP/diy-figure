package com.diyfigure.series.dto;

import com.diyfigure.common.enums.SpecTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新系列请求 DTO
 * 仅允许修改名称和尺寸档位,规格档位创建后不可更改
 */
@Data
public class SeriesUpdateRequest {

    @NotBlank(message = "系列名称不能为空")
    @Size(max = 100, message = "名称最多 100 个字符")
    private String name;

    @NotBlank(message = "尺寸档位不能为空")
    private String sizeTier;
}
