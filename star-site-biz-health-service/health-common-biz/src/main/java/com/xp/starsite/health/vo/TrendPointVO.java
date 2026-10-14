package com.xp.starsite.health.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 趋势点：某成员某指标的某一年的值
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class TrendPointVO {

    /** 报告年份 */
    private Integer reportYear;

    /** 报告 id */
    private Long reportId;

    /** 数值结果（与 valueText 二选一有值） */
    private BigDecimal valueNum;

    /** 文本结果（阴/阳等，与 valueNum 二选一有值） */
    private String valueText;

    /** 单位 */
    private String unit;

    /** 参考下限（趋势图参考区间带） */
    private BigDecimal refLow;

    /** 参考上限 */
    private BigDecimal refHigh;

    /** 0正常 1偏高 2偏低 */
    private Integer abnormalFlag;
}
