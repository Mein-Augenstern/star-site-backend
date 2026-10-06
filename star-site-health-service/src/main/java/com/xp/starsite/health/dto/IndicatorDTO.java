package com.xp.starsite.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单条指标入参
 */
@Data
public class IndicatorDTO {

    @NotBlank(message = "指标名不能为空")
    @Size(max = 64, message = "指标名最长 64 位")
    private String itemName;

    @Size(max = 32, message = "分组最长 32 位")
    private String itemGroup;

    private BigDecimal valueNum;

    @Size(max = 64, message = "文本结果最长 64 位")
    private String valueText;

    @Size(max = 32, message = "单位最长 32 位")
    private String unit;

    private BigDecimal refLow;

    private BigDecimal refHigh;

    private Integer sortNo;
}
