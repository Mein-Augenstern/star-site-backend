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

    /**
     * 事件 → 规则 upsert（幂等键 source+bizType+bizId），并立即补生成首期实例
     *
     * @param event 提醒注册事件（来源服务/业务类型/业务 id/提醒计划）
     */
    void upsertRule(ReminderRegisterEvent event);

    /**
     * 内部 Feign：跨服务 upsert 提醒规则（幂等）
     *
     * @param dto 跨服务提醒规则
     */
    void upsertRuleFromInternal(com.xp.starsite.api.dto.ReminderRuleDTO dto);

    /**
     * 内部 Feign：跨服务删除提醒规则
     *
     * @param dto 跨服务提醒规则（按幂等键定位）
     */
    void deleteRuleFromInternal(com.xp.starsite.api.dto.ReminderRuleDTO dto);

    /**
     * 事件 → 撤销规则（物理删）与未完成实例
     *
     * @param event 提醒撤销事件（按幂等键定位规则；规则不存在时静默返回）
     */
    void cancelRule(ReminderCancelEvent event);

    /**
     * 用户自建规则（source=user，biz_id=规则自身 id）：归属校验后落规则并立即补生成 30 天实例
     *
     * @param memberId 当前登录账号
     * @param dto      规则创建参数（归属家庭成员/标题/首提醒日/重复类型）
     * @return 新建规则视图（含待办实例数）
     * @throws com.xp.starsite.common.exception.BizException 归属校验失败（403）或家庭服务不可用
     */
    com.xp.starsite.reminder.vo.UserRuleVO createUserRule(Long memberId,
            com.xp.starsite.reminder.dto.UserRuleCreateDTO dto);

    /**
     * 我的家庭成员自建规则列表
     *
     * @param memberId 当前登录账号
     * @return 规则列表（含每条规则的待办实例数）
     */
    List<com.xp.starsite.reminder.vo.UserRuleVO> listUserRules(Long memberId);

    /**
     * 删除自建规则（清未完成实例；已投递通知保留）
     *
     * @param memberId 当前登录账号（用于归属校验）
     * @param ruleId   规则 id
     * @throws com.xp.starsite.common.exception.BizException 规则不存在或非自建规则（404）、无权限操作（403）
     */
    void deleteUserRule(Long memberId, Long ruleId);

    /**
     * 扫描：为启用规则预生成未来 N 天实例（幂等），并把今天到期的待提醒实例投递为站内通知。
     * 定时任务与 internal 手动触发共用。
     *
     * @return 本次新投递的通知数
     */
    int scanAndNotify();

    /**
     * 某账号未来 N 天的提醒（含已通知/待提醒，倒序）
     *
     * @param memberId 当前登录账号
     * @param days     展望天数
     * @return 提醒实例视图列表
     */
    List<OccurrenceVO> upcoming(Long memberId, int days);

    /**
     * 未读通知数（铃铛角标）
     *
     * @param memberId 当前登录账号
     * @return 未读通知条数
     */
    long unreadCount(Long memberId);

    /**
     * 通知列表（分页按时间倒序）
     *
     * @param memberId 当前登录账号
     * @param page     页码（从 1 起）
     * @param size     每页条数
     * @return 通知视图列表
     */
    List<NotificationVO> notifications(Long memberId, long page, long size);

    /**
     * 标记通知已读
     *
     * @param memberId       当前登录账号（归属不匹配时 no-op）
     * @param notificationId 通知 id
     */
    void markRead(Long memberId, Long notificationId);

    /**
     * 实例标记完成
     *
     * @param memberId     当前登录账号（归属不匹配时 no-op）
     * @param occurrenceId 提醒实例 id
     */
    void completeOccurrence(Long memberId, Long occurrenceId);

    /**
     * 实例标记忽略
     *
     * @param memberId     当前登录账号（归属不匹配时 no-op）
     * @param occurrenceId 提醒实例 id
     */
    void dismissOccurrence(Long memberId, Long occurrenceId);
}
