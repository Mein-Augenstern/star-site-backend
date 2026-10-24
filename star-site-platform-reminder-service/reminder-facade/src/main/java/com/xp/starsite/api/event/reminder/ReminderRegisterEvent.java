package com.xp.starsite.api.event.reminder;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 注册/更新提醒规则（全量 upsert 语义）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReminderRegisterEvent extends ReminderEvent {

    /** 归属家庭 id */
    private Long familyId;

    /** 归属顶梁柱账号 id */
    private Long ownerMemberId;

    /** 提醒标题，如「XX 重疾险 缴费」 */
    private String title;

    /** 首次提醒日期 */
    private LocalDate firstRemindAt;

    /** 重复类型，见 {@link ReminderRepeatType} */
    private ReminderRepeatType repeatType;

    /** 重复截止日（如保单到期后不再提醒缴费），可空 = 无限期 */
    private LocalDate repeatUntil;

    /** 提前天数（0 = 当天提醒） */
    private Integer advanceDays;
}
