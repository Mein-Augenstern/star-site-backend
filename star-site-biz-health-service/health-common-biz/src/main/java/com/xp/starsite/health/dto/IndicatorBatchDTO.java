package com.xp.starsite.health.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量录入指标入参
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class IndicatorBatchDTO {

    /** 本次批量录入的指标列表 */
    @Valid
    @NotEmpty(message = "指标列表不能为空")
    private List<IndicatorDTO> indicators;
}
