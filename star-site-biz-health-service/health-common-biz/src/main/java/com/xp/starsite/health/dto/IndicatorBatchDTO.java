package com.xp.starsite.health.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量录入指标入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class IndicatorBatchDTO {

    @Valid
    @NotEmpty(message = "指标列表不能为空")
    private List<IndicatorDTO> indicators;
}
