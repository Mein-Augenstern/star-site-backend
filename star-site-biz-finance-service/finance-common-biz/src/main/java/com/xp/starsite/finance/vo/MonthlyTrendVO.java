package com.xp.starsite.finance.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/** 月度收支趋势点（近 12 个月）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
@AllArgsConstructor
public class MonthlyTrendVO {

    /** YYYY-MM */
    private String month;

    /** 当月收入合计（元） */
    private BigDecimal income;

    /** 当月支出合计（元） */
    private BigDecimal expense;
}
