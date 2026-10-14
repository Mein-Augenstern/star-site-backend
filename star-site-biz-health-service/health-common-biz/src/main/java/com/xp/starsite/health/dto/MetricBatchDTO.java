package com.xp.starsite.health.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 日常指标批量录入（血压一次产生 bp_sys + bp_dia 两条）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class MetricBatchDTO {

    /** 本次批量录入的指标项列表 */
    @Valid
    @NotEmpty(message = "指标不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        /** 家庭成员 id */
        @NotNull(message = "家庭成员不能为空")
        private Long familyMemberId;

        /** 测量日期 */
        @NotNull(message = "测量日期不能为空")
        private LocalDate metricDate;

        /** weight / bp_sys / bp_dia / glucose / heart_rate */
        @NotBlank(message = "指标类型不能为空")
        private String metricType;

        /** 数值 */
        @NotNull(message = "数值不能为空")
        private BigDecimal valueNum;

        /** 备注 */
        private String remark;
    }
}
