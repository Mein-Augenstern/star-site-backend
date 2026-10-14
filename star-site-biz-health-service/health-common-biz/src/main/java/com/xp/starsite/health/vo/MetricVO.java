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

    /** 主键 */
    private Long id;

    /** 家庭成员 id */
    private Long familyMemberId;

    /** 测量日期 */
    private LocalDate metricDate;

    /** 指标类型（weight/bp_sys/bp_dia/glucose/heart_rate） */
    private String metricType;

    /** 数值 */
    private BigDecimal valueNum;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
