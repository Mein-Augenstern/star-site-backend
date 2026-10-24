package com.xp.starsite.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单条指标入参
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class IndicatorDTO {

    /** 指标名称（如 空腹血糖） */
    @NotBlank(message = "指标名不能为空")
    @Size(max = 64, message = "指标名最长 64 位")
    private String itemName;

    /** 指标分组（如 血脂四项） */
    @Size(max = 32, message = "分组最长 32 位")
    private String itemGroup;

    /** 数值结果（与 valueText 二选一） */
    private BigDecimal valueNum;

    /** 文本结果（阴/阳等，与 valueNum 二选一） */
    @Size(max = 64, message = "文本结果最长 64 位")
    private String valueText;

    /** 单位（如 mmol/L） */
    @Size(max = 32, message = "单位最长 32 位")
    private String unit;

    /** 参考区间下限 */
    private BigDecimal refLow;

    /** 参考区间上限 */
    private BigDecimal refHigh;

    /** 排序号（报告内展示顺序） */
    private Integer sortNo;
}
