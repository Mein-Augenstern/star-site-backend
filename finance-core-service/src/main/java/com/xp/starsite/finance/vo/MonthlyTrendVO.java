package com.xp.starsite.finance.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/** 月度收支趋势点（近 12 个月）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@AllArgsConstructor
public class MonthlyTrendVO {

    /** YYYY-MM */
    private String month;

    private BigDecimal income;

    private BigDecimal expense;
}
