package com.xp.starsite.reminder.converter;

import com.xp.starsite.reminder.dal.dataobject.ReminderOccurrenceDO;
import com.xp.starsite.reminder.entity.ReminderOccurrence;
import org.springframework.beans.BeanUtils;

/**
 * 提醒实例模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class ReminderOccurrenceConverter {

    private ReminderOccurrenceConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static ReminderOccurrenceDO toDO(ReminderOccurrence occurrence) {
        ReminderOccurrenceDO row = new ReminderOccurrenceDO();
        row.setId(occurrence.getId());
        row.setRuleId(occurrence.getRuleId());
        row.setFamilyId(occurrence.getFamilyId());
        row.setOwnerMemberId(occurrence.getOwnerMemberId());
        row.setTitle(occurrence.getTitle());
        row.setSource(occurrence.getSource());
        row.setBizType(occurrence.getBizType());
        row.setBizId(occurrence.getBizId());
        row.setDueDate(occurrence.getDueDate());
        row.setStatus(occurrence.getStatus());
        row.setNotifiedAt(occurrence.getNotifiedAt());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static ReminderOccurrence toEntity(ReminderOccurrenceDO row) {
        ReminderOccurrence occurrence = new ReminderOccurrence();
        BeanUtils.copyProperties(row, occurrence);
        return occurrence;
    }
}
