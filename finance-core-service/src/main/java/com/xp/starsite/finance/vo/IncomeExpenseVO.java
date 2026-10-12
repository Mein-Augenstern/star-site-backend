package com.xp.starsite.finance.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收支表（期间内；调整/转账不计收支）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class IncomeExpenseVO {

    private int year;

    /** 0 = 全年 */
    private Integer month;

    private BigDecimal income;

    private BigDecimal expense;

    /** 结余 = 收入 - 支出 */
    private BigDecimal surplus;

    /** 结余率（收入为 0 时 null） */
    private BigDecimal surplusRate;
}
