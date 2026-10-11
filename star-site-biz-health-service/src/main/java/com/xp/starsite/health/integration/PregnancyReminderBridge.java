package com.xp.starsite.health.integration;

import com.xp.starsite.api.client.ReminderApi;
import com.xp.starsite.api.dto.ReminderRuleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 提醒中心集成层（SOFA integration 层）：health 域对外部提醒域的唯一出口。
 * 领域内其他类禁止直接依赖提醒域的 Feign/Mapper。
 *
 * 职责：将孕期产检里程碑自动注册为提醒中心的待办规则（幂等 upsert）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PregnancyReminderBridge {

    private final ReminderApi reminderApi;

    /** 产检窗口的提前提醒天数（提前 7 天进入待办） */
    private static final int ADVANCE_DAYS = 7;

    /**
     * 依据孕周（LMP）批量生成产检里程碑提醒规则。
     * 幂等：source+bizType+bizId 固定，重复调用 upsert 无副作用；
     * 已过窗口的里程碑自动不再生成。
     *
     * @param familyMemberId 孕妇的家庭成员 id
     * @param lmpDate        末次月经第一天（孕周锚点）
     * @param familyId       家庭 id
     * @param ownerMemberId  归属账号
     * @return 注册的提醒规则数
     */
    public int registerPrenatalCheckups(Long familyMemberId, LocalDate lmpDate,
                                        Long familyId, Long ownerMemberId) {
        List<ReminderRuleDTO> rules = buildMilestoneRules(familyMemberId, lmpDate, familyId, ownerMemberId);
        int registered = 0;
        for (ReminderRuleDTO rule : rules) {
            try {
                reminderApi.upsertRule(rule);
                registered++;
            } catch (Exception e) {
                // 单条失败不阻断整批（提醒是增值能力，非强一致）
                log.error("产检提醒注册失败: familyMemberId={}, title={}", familyMemberId,
                        rule.getTitle(), e);
            }
        }
        return registered;
    }

    private List<ReminderRuleDTO> buildMilestoneRules(Long familyMemberId, LocalDate lmpDate,
                                                      Long familyId, Long ownerMemberId) {
        List<ReminderRuleDTO> rules = new ArrayList<>();
        // 标准产检里程碑：{孕周窗口起点, 名称, 说明}（与 MILESTONES 展示口径一致）
        Object[][] milestones = {
                {6, "首次检查·建档B超", "确认宫内孕、胎心"},
                {11, "NT 检查（早期唐筛）", "孕 11-13+6 窗口，勿错过"},
                {15, "唐筛 / 无创 DNA", "中期血清学筛查"},
                {20, "大排畸（系统超声）", "胎儿结构筛查"},
                {24, "糖耐量试验 OGTT", "妊娠糖尿病筛查，需空腹"},
                {28, "小排畸·胎心", "复查生长发育"},
                {32, "胎心监护起步", "每 2 周产检"},
                {37, "足月·每周产检", "随时准备待产"},
        };
        LocalDate today = LocalDate.now();
        for (Object[] m : milestones) {
            int fromWeek = (int) m[0];
            String name = (String) m[1];
            String tip = (String) m[2];
            LocalDate remindDate = lmpDate.plusWeeks(fromWeek);
            if (remindDate.isBefore(today)) {
                continue; // 已过窗口的不再注册
            }
            ReminderRuleDTO rule = new ReminderRuleDTO();
            rule.setSource("star-site-biz-health");
            rule.setBizType("prenatal_checkup");
            rule.setBizId(fromWeek * 100L + familyMemberId);
            rule.setFamilyId(familyId);
            rule.setFamilyMemberId(familyMemberId);
            rule.setTitle(String.format("孕 %d 周：%s（%s）", fromWeek, name, tip));
            rule.setRemindDate(remindDate);
            rule.setRepeatType("NONE");
            rule.setOwnerMemberId(ownerMemberId);
            rule.setAdvanceDays(ADVANCE_DAYS);
            rule.setRemark(tip);
            rules.add(rule);
        }
        return rules;
    }
}
