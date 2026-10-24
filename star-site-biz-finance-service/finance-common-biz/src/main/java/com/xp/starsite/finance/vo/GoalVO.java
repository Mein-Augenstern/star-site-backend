package com.xp.starsite.finance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 目标视图（含进度）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class GoalVO {

    /** 目标 id */
    private Long id;

    /** 目标名称 */
    private String name;

    /** 目标金额（元） */
    private BigDecimal targetAmount;

    /** 当前已存金额（元） */
    private BigDecimal currentAmount;

    /** 目标截止日 */
    private LocalDate deadline;

    /** 进度 0-1（target 为 0 时 null） */
    private BigDecimal progress;

    /** 备注 */
    private String remark;
}
