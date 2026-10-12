package com.xp.starsite.health.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日常健康指标数据对象（daily_metric 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/DailyMetric。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class DailyMetricDO {

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

    /** 逻辑删标志 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
