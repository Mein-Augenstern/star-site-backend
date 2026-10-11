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

    private String category;

    private String categoryName;

    private BigDecimal amount;
}
