package com.xp.starsite.recheck.assembler;

import com.xp.starsite.api.dto.ReminderRuleDTO;
import com.xp.starsite.api.dto.health.HealthReportDTO;
import com.xp.starsite.recheck.entity.RecheckPlan;
import com.xp.starsite.api.event.reminder.ReminderRepeatType;

/**
 * 复查提醒规则参数组装器（RPC 参数组装收敛在 core-service，integration 只负责调用+容错）。
 * 一条复查计划挂两条 reminder 规则（幂等键 source+bizType+bizId，bizId=计划 id）：
 *  - recheck_review：复查日一次性提醒（提前 7 天）；
 *  - recheck_daily：每日监测提醒（挂 metricType 时，至目标复查日截止）。
 * 纯 POJO——只构造 DTO 不发起调用。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class ReminderRuleAssembler {

    /** 规则来源标识（幂等键第一段） */
    public static final String SOURCE = "star-site-biz-recheck";

    /** 业务类型：复查日一次性提醒 */
    public static final String BIZ_TYPE_REVIEW = "recheck_review";

    /** 业务类型：每日监测提醒 */
    public static final String BIZ_TYPE_DAILY = "recheck_daily";

    private ReminderRuleAssembler() {
    }

    /** 是否挂每日监测（计划关联了日常指标类型） */
    public static boolean hasDailyMonitoring(RecheckPlan plan) {
        return plan.getMetricType() != null && !plan.getMetricType().isBlank();
    }

    /** 复查日一次性提醒规则 */
    public static ReminderRuleDTO reviewRule(RecheckPlan plan, String memberName) {
        ReminderRuleDTO dto = base(plan, BIZ_TYPE_REVIEW);
        dto.setTitle(memberName + "「" + plan.getItemName() + "」复查");
        dto.setRemindDate(plan.getTargetDate());
        dto.setRepeatType(ReminderRepeatType.NONE.name());
        dto.setAdvanceDays(7);
        dto.setRemark("复查闭环：对照基线 " + baselineText(plan));
        return dto;
    }

    /** 每日监测提醒规则（首次提醒明天起，至复查日截止） */
    public static ReminderRuleDTO dailyRule(RecheckPlan plan) {
        ReminderRuleDTO dto = base(plan, BIZ_TYPE_DAILY);
        dto.setTitle("每日监测：" + plan.getItemName());
        dto.setRemindDate(java.time.LocalDate.now().plusDays(1));
        dto.setRepeatType(ReminderRepeatType.DAILY.name());
        dto.setRepeatUntil(plan.getTargetDate());
        dto.setAdvanceDays(0);
        dto.setRemark("复查前每日监测，至 " + plan.getTargetDate() + " 截止");
        return dto;
    }

    /** 撤销用幂等键（仅 source/bizType/bizId 参与定位） */
    public static ReminderRuleDTO deleteKey(RecheckPlan plan, String bizType) {
        return base(plan, bizType);
    }

    private static ReminderRuleDTO base(RecheckPlan plan, String bizType) {
        ReminderRuleDTO dto = new ReminderRuleDTO();
        dto.setSource(SOURCE);
        dto.setBizType(bizType);
        dto.setBizId(plan.getId());
        dto.setFamilyId(plan.getFamilyId());
        dto.setFamilyMemberId(plan.getFamilyMemberId());
        dto.setOwnerMemberId(plan.getOwnerMemberId());
        return dto;
    }

    /** 基线展示文案（数值/文本 + 参考区间）， remark 用 */
    private static String baselineText(RecheckPlan plan) {
        String value = plan.getBaselineValueText() != null
                ? plan.getBaselineValueText()
                : (plan.getBaselineValueNum() == null ? "-" : plan.getBaselineValueNum().toPlainString());
        String range = HealthReportDTO.refRangeText(plan.getRefLow(), plan.getRefHigh());
        return value + (range.isEmpty() ? "" : "（" + range + "）");
    }
}
