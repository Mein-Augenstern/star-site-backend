package com.xp.starsite.reminder.converter;

import com.xp.starsite.reminder.dal.dataobject.ReminderRuleDO;
import com.xp.starsite.reminder.entity.ReminderRule;
import org.springframework.beans.BeanUtils;

/**
 * 提醒规则模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class ReminderRuleConverter {

    private ReminderRuleConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static ReminderRuleDO toDO(ReminderRule rule) {
        ReminderRuleDO row = new ReminderRuleDO();
        row.setId(rule.getId());
        row.setFamilyId(rule.getFamilyId());
        row.setFamilyMemberId(rule.getFamilyMemberId());
        row.setOwnerMemberId(rule.getOwnerMemberId());
        row.setSource(rule.getSource());
        row.setBizType(rule.getBizType());
        row.setBizId(rule.getBizId());
        row.setTitle(rule.getTitle());
        row.setFirstRemindAt(rule.getFirstRemindAt());
        row.setRepeatType(rule.getRepeatType());
        row.setRepeatUntil(rule.getRepeatUntil());
        row.setAdvanceDays(rule.getAdvanceDays());
        row.setStatus(rule.getStatus());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static ReminderRule toEntity(ReminderRuleDO row) {
        ReminderRule rule = new ReminderRule();
        BeanUtils.copyProperties(row, rule);
        return rule;
    }
}
