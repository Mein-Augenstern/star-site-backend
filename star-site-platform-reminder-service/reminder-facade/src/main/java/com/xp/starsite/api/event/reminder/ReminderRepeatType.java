package com.xp.starsite.api.event.reminder;

/**
 * 提醒重复类型
 *
 * @author muyi
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

    /** 每天（复查闭环的每日监测打卡等，通常配合 repeatUntil 截止） */
    DAILY,

    /** 自定义（间隔天数，暂未支持） */
    CUSTOM;

    /**
     * 宽松解析：null/空白/未知值一律回落 NONE（单次）。
     * repeat_type 在库里是字符串，历史数据与外部输入不保证合法，解析不允许抛异常。
     */
    public static ReminderRepeatType ofLenient(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
