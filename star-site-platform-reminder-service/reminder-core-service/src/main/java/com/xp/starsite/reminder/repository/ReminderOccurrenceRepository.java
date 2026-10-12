package com.xp.starsite.reminder.repository;

import com.xp.starsite.reminder.entity.ReminderOccurrence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 提醒实例仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface ReminderOccurrenceRepository {

    /** 保存（新增；uk_rule_due 冲突抛 DuplicateKeyException，并发幂等兜底） */
    ReminderOccurrence save(ReminderOccurrence occurrence);

    /** 按 id 取实例 */
    Optional<ReminderOccurrence> findById(Long id);

    /** 实例是否已生成（生成前防重检查，与 uk_rule_due 双保险） */
    int countByRuleAndDueDate(Long ruleId, LocalDate dueDate);

    /** 规则名下未完成实例数（待提醒/已通知，用户规则列表的待办角标） */
    long countActiveByRuleId(Long ruleId);

    /** 规则撤销：物理删未完成实例（已完成/已忽略保留） */
    void deleteActiveByRuleId(Long ruleId);

    /** 每日扫描投递：到期（due_date ≤ 指定日）且待提醒的实例 */
    List<ReminderOccurrence> selectDuePendingUpTo(LocalDate dueDate);

    /** 我的近期安排：归属账号、截止日前、未完成，按应提醒日升序 */
    List<ReminderOccurrence> selectUpcomingByOwner(Long ownerMemberId, LocalDate dueBefore);

    /** 投递完成置位：status 1→2 + 通知时间（仅待提醒实例可置位） */
    void markNotified(Long id, LocalDateTime notifiedAt);

    /** 显式状态更新：完成(3)/忽略(4) */
    void updateStatus(Long id, int status);
}
