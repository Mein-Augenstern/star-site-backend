package com.xp.starsite.recheck.integration;

import com.xp.starsite.api.client.ReminderApi;
import com.xp.starsite.api.dto.ReminderRuleDTO;
import com.xp.starsite.api.dto.health.HealthReportDTO;
import com.xp.starsite.recheck.entity.RecheckPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 提醒中心集成层（SOFA integration 层）：复查计划对外部提醒域的唯一出口。
 * 领域内其他类禁止直接依赖 reminder 的 Feign。
 * 一条计划挂两条规则（幂等键 source+bizType+bizId，bizId=计划 id）：
 *  - recheck_review：复查日一次性提醒（提前 7 天）；
 *  - recheck_daily：每日监测提醒（挂 metricType 时，至目标复查日截止）。
 * 与 health 的产检提醒同一哲学：单条失败不阻断主流程（提醒是增值能力，非强一致）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderIntegration {

    /** 规则来源标识（幂等键第一段） */
    public static final String SOURCE = "star-site-biz-recheck";

    /** 业务类型：复查日一次性提醒 */
    public static final String BIZ_TYPE_REVIEW = "recheck_review";

    /** 业务类型：每日监测提醒 */
    public static final String BIZ_TYPE_DAILY = "recheck_daily";

    private final ReminderApi reminderApi;

    /** 计划建档后注册提醒规则（复查日 + 可选每日监测） */
    public void registerRules(RecheckPlan plan, String memberName) {
        ReminderRuleDTO review = new ReminderRuleDTO();
        review.setSource(SOURCE);
        review.setBizType(BIZ_TYPE_REVIEW);
        review.setBizId(plan.getId());
        review.setFamilyId(plan.getFamilyId());
        review.setFamilyMemberId(plan.getFamilyMemberId());
        review.setOwnerMemberId(plan.getOwnerMemberId());
        review.setTitle(memberName + "「" + plan.getItemName() + "」复查");
        review.setRemindDate(plan.getTargetDate());
        review.setRepeatType("NONE");
        review.setAdvanceDays(7);
        review.setRemark("复查闭环：对照基线 " + baselineText(plan));
        upsertTolerant(review, plan);

        if (hasDailyMonitoring(plan)) {
            ReminderRuleDTO daily = new ReminderRuleDTO();
            daily.setSource(SOURCE);
            daily.setBizType(BIZ_TYPE_DAILY);
            daily.setBizId(plan.getId());
            daily.setFamilyId(plan.getFamilyId());
            daily.setFamilyMemberId(plan.getFamilyMemberId());
            daily.setOwnerMemberId(plan.getOwnerMemberId());
            daily.setTitle("每日监测：" + plan.getItemName());
            daily.setRemindDate(LocalDate.now().plusDays(1));
            daily.setRepeatType("DAILY");
            daily.setRepeatUntil(plan.getTargetDate());
            daily.setAdvanceDays(0);
            daily.setRemark("复查前每日监测，至 " + plan.getTargetDate() + " 截止");
            upsertTolerant(daily, plan);
        }
    }

    /** 计划闭环/取消后撤销全部规则（幂等键定位，不存在静默成功） */
    public void cancelRules(RecheckPlan plan) {
        deleteTolerant(buildKey(plan, BIZ_TYPE_REVIEW), plan);
        if (hasDailyMonitoring(plan)) {
            deleteTolerant(buildKey(plan, BIZ_TYPE_DAILY), plan);
        }
    }

    private ReminderRuleDTO buildKey(RecheckPlan plan, String bizType) {
        ReminderRuleDTO dto = new ReminderRuleDTO();
        dto.setSource(SOURCE);
        dto.setBizType(bizType);
        dto.setBizId(plan.getId());
        return dto;
    }

    private boolean hasDailyMonitoring(RecheckPlan plan) {
        return plan.getMetricType() != null && !plan.getMetricType().isBlank();
    }

    private String baselineText(RecheckPlan plan) {
        String value = plan.getBaselineValueText() != null
                ? plan.getBaselineValueText()
                : (plan.getBaselineValueNum() == null ? "-" : plan.getBaselineValueNum().toPlainString());
        String range = HealthReportDTO.refRangeText(plan.getRefLow(), plan.getRefHigh());
        return value + (range.isEmpty() ? "" : "（" + range + "）");
    }

    private void upsertTolerant(ReminderRuleDTO dto, RecheckPlan plan) {
        try {
            reminderApi.upsertRule(dto);
        } catch (Exception e) {
            log.error("复查提醒规则注册失败（不阻断主流程）: planId={}, bizType={}",
                    plan.getId(), dto.getBizType(), e);
        }
    }

    private void deleteTolerant(ReminderRuleDTO key, RecheckPlan plan) {
        try {
            reminderApi.deleteRule(key);
        } catch (Exception e) {
            log.error("复查提醒规则撤销失败（不阻断主流程）: planId={}, bizType={}",
                    plan.getId(), key.getBizType(), e);
        }
    }
}
