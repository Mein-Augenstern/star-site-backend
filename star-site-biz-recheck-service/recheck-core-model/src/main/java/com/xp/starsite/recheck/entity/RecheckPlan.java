package com.xp.starsite.recheck.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 复查计划（recheck_plan）：垂直产品「复查闭环」的主体表。
 * 一条计划 = 一份报告里的一个异常指标：拍报告 → AI 解读发现异常 → 建本计划
 * （目标复查日 + 复查提醒 + 可选每日监测规则，经 reminder 横向服务）→ 复查拍新报告
 * → 按指标名对齐基线给出对比结论 → 计划完结、规则撤销。
 * 报告/指标数据全在 health（横向服务），本表只存基线快照与闭环状态。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class RecheckPlan {

    /** 进行中（已挂提醒/监测规则） */
    public static final int STATUS_ACTIVE = 1;

    /** 已闭环（关联了复查新报告并给出对比结论） */
    public static final int STATUS_CLOSED = 2;

    /** 已取消（撤销提醒/监测规则） */
    public static final int STATUS_CANCELLED = 3;

    /** 雪花 id（应用侧赋值）：reminder 规则的 bizId 需要在 insert 前拿到 */
    private Long id;

    /** 家庭 id（多人协作预留冗余） */
    private Long familyId;

    /** 归属账号 id（创建计划的顶梁柱） */
    private Long ownerMemberId;

    /** 目标成员档案 id（family 服务自然人） */
    private Long familyMemberId;

    /** 基线报告 id（health 服务） */
    private Long sourceReportId;

    /** 基线报告标题快照（免跨服务查列表） */
    private String sourceReportTitle;

    /** 基线指标 id（health 服务 health_indicator） */
    private Long sourceIndicatorId;

    /** 指标名（如 低密度脂蛋白胆固醇）：闭环时按名在新报告中对齐 */
    private String itemName;

    /** 基线数值（数值型指标） */
    private BigDecimal baselineValueNum;

    /** 基线结果文本（定性指标如 尿蛋白 ±） */
    private String baselineValueText;

    /** 单位快照 */
    private String unit;

    /** 参考区间下限快照 */
    private BigDecimal refLow;

    /** 参考区间上限快照 */
    private BigDecimal refHigh;

    /** 基线异常标志：0正常 1偏高 2偏低 */
    private Integer baselineAbnormalFlag;

    /** 目标复查日期（默认建计划日 +90 天） */
    private LocalDate targetDate;

    /** 关联日常监测指标类型（bp_sys/bp_dia/weight/glucose...，可空=不挂每日监测） */
    private String metricType;

    /** 状态：1进行中 2已闭环 3已取消 */
    private Integer status;

    /** 复查新报告 id（闭环时回填） */
    private Long closedReportId;

    /** 复查新报告标题快照 */
    private String closedReportTitle;

    /** 闭环对比结论（一句话，生成时写入） */
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
