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
 */
@Data
public class MetricBatchDTO {

    @Valid
    @NotEmpty(message = "指标不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "家庭成员不能为空")
        private Long familyMemberId;

        @NotNull(message = "测量日期不能为空")
        private LocalDate metricDate;

        /** weight / bp_sys / bp_dia / glucose / heart_rate */
        @NotBlank(message = "指标类型不能为空")
        private String metricType;

        @NotNull(message = "数值不能为空")
        private BigDecimal valueNum;

        private String remark;
    }
}
