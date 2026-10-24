package com.xp.starsite.health.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体检指标视图
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class HealthIndicatorVO {

    /** 主键（指标 id） */
    private Long id;

    /** 所属报告 id */
    private Long reportId;

    /** 报告年份（冗余） */
    private Integer reportYear;

    /** 指标名称（如 空腹血糖） */
    private String itemName;

    /** 指标分组（如 血脂四项） */
    private String itemGroup;

    /** 数值结果（与 valueText 二选一有值） */
    private BigDecimal valueNum;

    /** 文本结果（阴/阳等，与 valueNum 二选一有值） */
    private String valueText;

    /** 单位（如 mmol/L） */
    private String unit;

    /** 参考区间下限 */
    private BigDecimal refLow;

    /** 参考区间上限 */
    private BigDecimal refHigh;

    /** 0正常 1偏高 2偏低 */
    private Integer abnormalFlag;

    /** 排序号（报告内展示顺序） */
    private Integer sortNo;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
