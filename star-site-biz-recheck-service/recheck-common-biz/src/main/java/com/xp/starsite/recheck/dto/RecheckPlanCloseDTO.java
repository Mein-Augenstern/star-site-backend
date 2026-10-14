package com.xp.starsite.recheck.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 复查计划闭环：关联复查新报告，按指标名对齐基线生成对比结论。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class RecheckPlanCloseDTO {

    /** 复查新报告 id（health 服务） */
    @NotNull(message = "复查报告不能为空")
    private Long reportId;
}
