package com.xp.starsite.finance.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算视图（含当月使用情况）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class BudgetVO {

    /** 预算 id */
    private Long id;

    /** 预算分类枚举 */
    private String category;

    /** 分类名快照 */
    private String categoryName;

    /** 预算月份 YYYY-MM */
    private String period;

    /** 预算金额（元） */
    private BigDecimal amount;

    /** 当月该分类已支出（流水聚合） */
    private BigDecimal spent;

    /** 剩余 = amount - spent（可为负 = 超支） */
    private BigDecimal remaining;

    /** 使用率 0-1+（超支 >1；amount 为 0 时 null） */
    private BigDecimal usageRate;
}
