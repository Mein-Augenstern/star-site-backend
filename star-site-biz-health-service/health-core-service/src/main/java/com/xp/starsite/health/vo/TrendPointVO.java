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

    private Integer reportYear;

    private Long reportId;

    private BigDecimal valueNum;

    private String valueText;

    private String unit;

    /** 参考下限（趋势图参考区间带） */
    private BigDecimal refLow;

    /** 参考上限 */
    private BigDecimal refHigh;

    /** 0正常 1偏高 2偏低 */
    private Integer abnormalFlag;
}
