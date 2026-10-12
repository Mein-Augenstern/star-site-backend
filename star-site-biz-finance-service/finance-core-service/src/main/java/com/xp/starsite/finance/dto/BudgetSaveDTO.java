package com.xp.starsite.finance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算创建/编辑入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class BudgetSaveDTO {

    @NotBlank(message = "分类不能为空")
    private String category;

    /** 预算月份 YYYY-MM */
    @NotBlank(message = "预算月份不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "月份格式须为 YYYY-MM")
    private String period;

    @NotNull(message = "预算金额不能为空")
    @Digits(integer = 14, fraction = 2, message = "金额最多两位小数")
    private BigDecimal amount;
}
