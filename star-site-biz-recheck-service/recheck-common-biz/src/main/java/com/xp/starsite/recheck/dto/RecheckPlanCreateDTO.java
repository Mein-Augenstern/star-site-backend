package com.xp.starsite.recheck.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 复查计划创建：从一份报告的一个指标发起复查闭环。
 * targetDate 不传默认 +90 天；metricType 传了则同时挂每日监测规则。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class RecheckPlanCreateDTO {

    /** 目标成员档案 id */
    @NotNull(message = "家庭成员不能为空")
    private Long familyMemberId;

    /** 基线报告 id（health 服务） */
    @NotNull(message = "报告不能为空")
    private Long reportId;

    /** 基线指标 id（报告详情里的指标项） */
    @NotNull(message = "指标不能为空")
    private Long indicatorId;

    /** 目标复查日期（可空，默认 +90 天） */
    private LocalDate targetDate;

    /** 关联日常监测指标类型（bp_sys/bp_dia/weight/glucose...，可空=不挂每日监测） */
    @jakarta.validation.constraints.Size(max = 32, message = "监测指标类型过长")
    private String metricType;
}
