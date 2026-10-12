package com.xp.starsite.health.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日常指标视图
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class MetricVO {

    private Long id;

    private Long familyMemberId;

    private LocalDate metricDate;

    private String metricType;

    private BigDecimal valueNum;

    private String remark;

    private LocalDateTime createdAt;
}
