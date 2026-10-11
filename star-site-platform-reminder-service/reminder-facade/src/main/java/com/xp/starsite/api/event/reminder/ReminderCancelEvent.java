package com.xp.starsite.api.event.reminder;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 撤销提醒规则（按幂等键删除）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReminderCancelEvent extends ReminderEvent {
}
