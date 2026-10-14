package com.xp.starsite.health.assembler;

import com.xp.starsite.api.dto.ReminderRuleDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 产检里程碑提醒规则参数组装器（RPC 参数组装收敛在 core-service，integration 只负责调用+容错）。
 * 依据孕周（LMP）生成标准产检里程碑的提醒规则 DTO；已过窗口的自动跳过。
 * 纯 POJO——只构造 DTO 不发起调用。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class PrenatalRuleAssembler {

    /** 产检窗口的提前提醒天数（提前 7 天进入待办） */
    private static final int ADVANCE_DAYS = 7;

    private PrenatalRuleAssembler() {
    }

    /**
     * 依据孕周批量生成产检里程碑提醒规则
     *
     * @param familyMemberId 孕妇的家庭成员 id
     * @param lmpDate        末次月经第一天（孕周锚点）
     * @param familyId       家庭 id
     * @param ownerMemberId  归属账号
     * @return 规则列表（已过窗口的里程碑不含在内）
     */
    public static List<ReminderRuleDTO> milestoneRules(Long familyMemberId, LocalDate lmpDate,
                                                       Long familyId, Long ownerMemberId) {
        List<ReminderRuleDTO> rules = new ArrayList<>();
        // 标准产检里程碑：{孕周窗口起点, 名称, 说明}（与孕期助手页面展示口径一致）
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
