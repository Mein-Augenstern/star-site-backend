package com.xp.starsite.health.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 孕期档案设置入参（同成员重复提交=覆盖更新） */
@Data
public class PregnancyUpsertDTO {

    @NotNull(message = "家庭成员不能为空")
    private Long familyMemberId;

    @NotNull(message = "末次月经日期不能为空")
    private LocalDate lmpDate;

    private BigDecimal heightCm;

    private BigDecimal prePregnancyWeight;
}
