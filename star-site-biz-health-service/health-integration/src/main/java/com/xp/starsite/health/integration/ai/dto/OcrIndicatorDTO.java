package com.xp.starsite.health.integration.ai.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * OCR 单项指标草稿（跨语言契约，与 Python OcrIndicator 对齐）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class OcrIndicatorDTO implements Serializable {

    /** 指标名（如 空腹血糖） */
    private String itemName;

    /** 指标分组（如 血脂四项），可空 */
    private String itemGroup;

    /** 数值结果（与 valueText 二选一） */
    private BigDecimal valueNum;

    /** 文本结果（定性指标如 尿蛋白±） */
    private String valueText;

    /** 单位，可空 */
    private String unit;

    /** 参考区间下限，可空 */
    private BigDecimal refLow;

    /** 参考区间上限，可空 */
    private BigDecimal refHigh;
}
