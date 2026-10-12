package com.xp.starsite.reminder.controller.actions;

/**
 * 提醒域动作名常量（模板留痕 action 字段，规范：服务.资源.动作）
 * <p>
 * 动作名会进模板留痕与 digest，只能新增、不能改名：字符串值与历史值逐字节一致。
 * 常量名取「资源.动作」段，省略服务前缀 reminder.。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class ReminderActions {

    // —— 待办看板 ——

    /** 未来 N 天要办的事 */
    public static final String UPCOMING = "reminder.upcoming";

    // —— 待办实例 ——

    /** 完成待办事项 */
    public static final String OCCURRENCE_DONE = "reminder.occurrence.done";

    /** 忽略待办事项 */
    public static final String OCCURRENCE_DISMISS = "reminder.occurrence.dismiss";

    // —— 站内通知 ——

    /** 未读通知数 */
    public static final String NOTIFICATION_UNREAD_COUNT = "reminder.notification.unreadCount";

    /** 通知分页列表 */
    public static final String NOTIFICATION_LIST = "reminder.notification.list";

    /** 标记通知已读 */
    public static final String NOTIFICATION_READ = "reminder.notification.read";

    // —— 自建提醒规则 ——

    /** 新建自建提醒 */
    public static final String RULE_CREATE = "reminder.rule.create";

    /** 自建提醒规则列表 */
    public static final String RULE_LIST = "reminder.rule.list";

    /** 删除自建提醒 */
    public static final String RULE_DELETE = "reminder.rule.delete";

    // —— 服务间内部接口 ——

    /** 手动触发到期扫描 */
    public static final String INTERNAL_SCAN = "reminder.internal.scan";

    private ReminderActions() {
    }
}
