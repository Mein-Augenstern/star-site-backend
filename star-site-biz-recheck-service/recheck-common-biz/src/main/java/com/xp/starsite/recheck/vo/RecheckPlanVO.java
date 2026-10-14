package com.xp.starsite.recheck.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 复查计划视图：列表与详情共用。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class RecheckPlanVO {

    /** 雪花 id */
    private Long id;

    /** 目标成员档案 id */
    private Long familyMemberId;

    /** 成员姓名（family 快照冗余，展示用） */
    private String memberName;

    /** 指标名（复查对象） */
    private String itemName;

    /** 基线结果（数值或文本） */
    private String baselineText;

    /** 参考区间文案（如 "3.5-5.7"） */
    private String refRange;

    /** 单位 */
    private String unit;

    /** 目标复查日期 */
    private LocalDate targetDate;

    /** 距目标复查日天数（负数=已过期） */
    private Long daysLeft;

    /** 状态：1进行中 2已闭环 3已取消 */
    private Integer status;

    /** 状态文案 */
    private String statusText;

    /** 关联日常监测指标类型（可空） */
    private String metricType;

    /** 基线报告 id */
    private Long sourceReportId;

    /** 基线报告标题快照 */
    private String sourceReportTitle;

    /** 复查新报告 id（闭环时回填） */
    private Long closedReportId;

    /** 复查新报告标题快照 */
    private String closedReportTitle;

    /** 闭环对比结论 */
    private String conclusion;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
