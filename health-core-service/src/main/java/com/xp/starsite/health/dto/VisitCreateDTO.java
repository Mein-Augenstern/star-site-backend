package com.xp.starsite.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 就医记录创建入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class VisitCreateDTO {

    @NotNull(message = "家庭成员不能为空")
    private Long familyMemberId;

    @NotNull(message = "就诊日期不能为空")
    private LocalDate visitDate;

    @NotBlank(message = "医院不能为空")
    @Size(max = 128, message = "医院名称过长")
    private String hospital;

    @Size(max = 64, message = "科室名称过长")
    private String department;

    @Size(max = 255, message = "诊断说明过长")
    private String diagnosis;

    @Size(max = 500, message = "用药摘要过长")
    private String medication;

    private BigDecimal cost;

    private String remark;
}
