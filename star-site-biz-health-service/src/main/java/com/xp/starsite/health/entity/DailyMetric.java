package com.xp.starsite.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日常健康指标（血压/血糖/体重/心率等高频自测数据）
 */
@Data
@TableName("daily_metric")
public class DailyMetric {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 家庭成员 id */
    private Long familyMemberId;

    /** 测量日期 */
    private LocalDate metricDate;

    /** weight / bp_sys / bp_dia / glucose / heart_rate */
    private String metricType;

    private BigDecimal valueNum;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
