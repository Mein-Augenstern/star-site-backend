package com.xp.starsite.finance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 目标创建/编辑入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class GoalSaveDTO {

    @NotBlank(message = "目标名称不能为空")
    /** 目标名称 */
    private String name;

    @NotNull(message = "目标金额不能为空")
    @Digits(integer = 14, fraction = 2, message = "金额最多两位小数")
    /** 目标金额（元） */
    private BigDecimal targetAmount;

    /** 当前已存金额（元） */
    private BigDecimal currentAmount;

    /** 目标截止日 */
    private LocalDate deadline;

    /** 备注 */
    private String remark;
}
