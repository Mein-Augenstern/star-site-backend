package com.xp.starsite.health.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 孕期档案设置入参（同成员重复提交=覆盖更新）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class PregnancyUpsertDTO {

    /** 家庭成员 id */
    @NotNull(message = "家庭成员不能为空")
    private Long familyMemberId;

    /** 末次月经第一天 */
    @NotNull(message = "末次月经日期不能为空")
    private LocalDate lmpDate;

    /** 身高 cm */
    private BigDecimal heightCm;

    /** 孕前体重 kg */
    private BigDecimal prePregnancyWeight;
}
