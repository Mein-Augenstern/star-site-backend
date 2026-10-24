package com.xp.starsite.health.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日常健康指标（daily_metric 表）：血压/血糖/体重/心率等按日自测数据。
 * 与报告指标（health_indicator）互不隶属；血压一次录入产生 bp_sys/bp_dia 两行。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class DailyMetric {

    /** 主键 */
    private Long id;

    /** 家庭成员 id（family 服务的自然人档案） */
    private Long familyMemberId;

    /** 测量日期 */
    private LocalDate metricDate;

    /** 指标类型：weight / bp_sys / bp_dia / glucose / heart_rate */
    private String metricType;

    /** 测量值 */
    private BigDecimal valueNum;

    /** 备注 */
    private String remark;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
