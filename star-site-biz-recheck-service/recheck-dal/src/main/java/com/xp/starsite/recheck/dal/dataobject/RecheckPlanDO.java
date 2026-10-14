package com.xp.starsite.recheck.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 复查计划数据对象（recheck_plan 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/RecheckPlan。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class RecheckPlanDO {

    /** 主键（雪花 id，应用侧赋值） */
    private Long id;

    /** 家庭 id */
    private Long familyId;

    /** 归属账号 id */
    private Long ownerMemberId;

    /** 目标成员档案 id */
    private Long familyMemberId;

    /** 基线报告 id */
    private Long sourceReportId;

    /** 基线报告标题快照 */
    private String sourceReportTitle;

    /** 基线指标 id */
    private Long sourceIndicatorId;

    /** 指标名 */
    private String itemName;

    /** 基线数值 */
    private BigDecimal baselineValueNum;

    /** 基线结果文本 */
    private String baselineValueText;

    /** 单位快照 */
    private String unit;

    /** 参考区间下限快照 */
    private BigDecimal refLow;

    /** 参考区间上限快照 */
    private BigDecimal refHigh;

    /** 基线异常标志：0正常 1偏高 2偏低 */
    private Integer baselineAbnormalFlag;

    /** 目标复查日期 */
    private LocalDate targetDate;

    /** 关联日常监测指标类型（可空） */
    private String metricType;

    /** 状态：1进行中 2已闭环 3已取消 */
    private Integer status;

    /** 复查新报告 id */
    private Long closedReportId;

    /** 复查新报告标题快照 */
    private String closedReportTitle;

    /** 闭环对比结论 */
    private String conclusion;

    /** 备注 */
    private String remark;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
