package com.xp.starsite.finance.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/** 分类支出占比（饼图点）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@AllArgsConstructor
public class CategoryShareVO {

    /** 分类枚举 */
    private String category;

    /** 分类名 */
    private String categoryName;

    /** 期间内该分类合计金额（元） */
    private BigDecimal amount;
}
