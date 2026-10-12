package com.xp.starsite.reminder.mapper;

import com.xp.starsite.reminder.dal.dataobject.ReminderOccurrenceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒实例数据访问（DAL）：SQL 全部收敛在 mapper XML。
 * 本表无 deleted 列——随规则撤销物理删未完成实例；(rule_id, due_date) 唯一键防重。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface ReminderOccurrenceMapper {

    /** 新建实例（id DB 自增回填；uk_rule_due 冲突抛 DuplicateKeyException，并发幂等兜底） */
    int insert(ReminderOccurrenceDO occurrence);

    /** 按 id 取实例，不存在返回 null */
    ReminderOccurrenceDO selectById(@Param("id") Long id);

    /** 实例是否已生成（生成前防重检查，与 uk_rule_due 双保险） */
    int countByRuleAndDueDate(@Param("ruleId") Long ruleId, @Param("dueDate") LocalDate dueDate);

    /** 规则名下未完成实例数（待提醒/已通知，用户规则列表的待办角标） */
    long countActiveByRuleId(@Param("ruleId") Long ruleId);

    /** 规则撤销：物理删未完成实例（1待提醒/2已通知；已完成/已忽略保留） */
    int deleteActiveByRuleId(@Param("ruleId") Long ruleId);

    /** 每日扫描投递：到期（due_date &le; 指定日）且待提醒的实例 */
    List<ReminderOccurrenceDO> selectDuePendingUpTo(@Param("dueDate") LocalDate dueDate);

    /** 我的近期安排：归属账号、due_date &le; 截止日、未完成（待提醒/已通知），按应提醒日升序 */
    List<ReminderOccurrenceDO> selectUpcomingByOwner(@Param("ownerMemberId") Long ownerMemberId,
                                                     @Param("dueBefore") LocalDate dueBefore);

    /** 投递完成置位：status 1→2 + 通知时间（仅待提醒实例可置位） */
    int markNotified(@Param("id") Long id, @Param("notifiedAt") LocalDateTime notifiedAt);

    /** 显式状态更新：完成(3)/忽略(4) */
    int updateStatus(@Param("id") Long id, @Param("status") int status);
}
