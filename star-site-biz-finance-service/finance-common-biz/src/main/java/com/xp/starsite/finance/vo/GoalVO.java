package com.xp.starsite.finance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 目标视图（含进度）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class GoalVO {

    private Long id;

    private String name;

    private BigDecimal targetAmount;

    private BigDecimal currentAmount;

    private LocalDate deadline;

    /** 进度 0-1（target 为 0 时 null） */
    private BigDecimal progress;

    private String remark;
}
