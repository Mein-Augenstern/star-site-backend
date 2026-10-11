package com.xp.starsite.reminder.service;

import com.xp.starsite.api.event.reminder.ReminderCancelEvent;
import com.xp.starsite.api.event.reminder.ReminderRegisterEvent;
import com.xp.starsite.reminder.vo.NotificationVO;
import com.xp.starsite.reminder.vo.OccurrenceVO;

import java.util.List;

/**
 * 提醒中心服务
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface ReminderService {

    /** 事件 → 规则 upsert（幂等键 source+bizType+bizId），并立即补生成首期实例 */
    void upsertRule(ReminderRegisterEvent event);

    /** 内部 Feign：跨服务 upsert 提醒规则（幂等） */
    void upsertRuleFromInternal(com.xp.starsite.api.dto.ReminderRuleDTO dto);

    /** 内部 Feign：跨服务删除提醒规则 */
    void deleteRuleFromInternal(com.xp.starsite.api.dto.ReminderRuleDTO dto);

    /** 事件 → 撤销规则（物理删）与未完成实例 */
    void cancelRule(ReminderCancelEvent event);

    /** 用户自建规则（source=user，biz_id=规则自身 id）：归属校验后落规则并立即补生成 30 天实例 */
    com.xp.starsite.reminder.vo.UserRuleVO createUserRule(Long memberId,
            com.xp.starsite.reminder.dto.UserRuleCreateDTO dto);

    /** 我的家庭成员自建规则列表 */
    List<com.xp.starsite.reminder.vo.UserRuleVO> listUserRules(Long memberId);

    /** 删除自建规则（清未完成实例；已投递通知保留） */
    void deleteUserRule(Long memberId, Long ruleId);

    /**
     * 扫描：为启用规则预生成未来 N 天实例（幂等），并把今天到期的待提醒实例投递为站内通知。
     * 定时任务与 internal 手动触发共用。
     *
     * @return 本次新投递的通知数
     */
    int scanAndNotify();

    /** 某账号未来 N 天的提醒（含已通知/待提醒，倒序） */
    List<OccurrenceVO> upcoming(Long memberId, int days);

    /** 未读通知数（铃铛角标） */
    long unreadCount(Long memberId);

    /** 通知列表（分页按时间倒序） */
    List<NotificationVO> notifications(Long memberId, long page, long size);

    /** 标记通知已读 */
    void markRead(Long memberId, Long notificationId);

    /** 实例标记完成 */
    void completeOccurrence(Long memberId, Long occurrenceId);

    /** 实例标记忽略 */
    void dismissOccurrence(Long memberId, Long occurrenceId);
}
