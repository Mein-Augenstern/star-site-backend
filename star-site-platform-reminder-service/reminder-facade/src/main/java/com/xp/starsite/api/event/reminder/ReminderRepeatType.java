package com.xp.starsite.api.event.reminder;

/**
 * 提醒重复类型
 *
 * @author xiaopu
 * @since 2026-10
 */
public enum ReminderRepeatType {

    /** 单次 */
    NONE,

    /** 每年（缴费日、生日、年检） */
    ANNUAL,

    /** 每月 */
    MONTHLY,

    /** 每周 */
    WEEKLY,

    /** 自定义（间隔天数，暂未支持） */
    CUSTOM
}
