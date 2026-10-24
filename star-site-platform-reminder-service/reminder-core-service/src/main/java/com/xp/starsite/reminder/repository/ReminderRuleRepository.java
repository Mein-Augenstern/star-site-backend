package com.xp.starsite.reminder.repository;

import com.xp.starsite.reminder.entity.ReminderRule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 提醒规则仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface ReminderRuleRepository {

    /** 保存（新增；回填自增 id） */
    ReminderRule save(ReminderRule rule);

    /** 按幂等键取规则（事件消费端 upsert：source+bizType+bizId 唯一） */
    Optional<ReminderRule> findByIdempotentKey(String source, String bizType, Long bizId);

    /** 按 id 取规则 */
    Optional<ReminderRule> findById(Long id);

    /** 显式更新规则全量业务列 */
    void update(ReminderRule rule);

    /** 撤销删除（物理删）：衍生实例/通知的清理由调用方另行处理 */
    void deleteById(Long id);

    /** 每日扫描：全部启用中的规则 */
    List<ReminderRule> selectEnabled();

    /** 用户自建规则列表：指定来源、归属成员集合内、启用中，按 id 倒序 */
    List<ReminderRule> selectActiveBySourceAndMembers(String source, Collection<Long> familyMemberIds);
}
