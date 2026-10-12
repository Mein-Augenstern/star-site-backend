package com.xp.starsite.reminder.repository.impl;

import com.xp.starsite.reminder.converter.ReminderRuleConverter;
import com.xp.starsite.reminder.dal.dataobject.ReminderRuleDO;
import com.xp.starsite.reminder.entity.ReminderRule;
import com.xp.starsite.reminder.mapper.ReminderRuleMapper;
import com.xp.starsite.reminder.repository.ReminderRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 提醒规则仓储实现：调用 dal 的 {@link ReminderRuleMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class ReminderRuleRepositoryImpl implements ReminderRuleRepository {

    private final ReminderRuleMapper ruleMapper;

    @Override
    public ReminderRule save(ReminderRule rule) {
        ReminderRuleDO row = ReminderRuleConverter.toDO(rule);
        ruleMapper.insert(row);
        rule.setId(row.getId());
        return rule;
    }

    @Override
    public Optional<ReminderRule> findByIdempotentKey(String source, String bizType, Long bizId) {
        return Optional.ofNullable(ruleMapper.selectByIdempotentKey(source, bizType, bizId))
                .map(ReminderRuleConverter::toEntity);
    }

    @Override
    public Optional<ReminderRule> findById(Long id) {
        return Optional.ofNullable(ruleMapper.selectById(id)).map(ReminderRuleConverter::toEntity);
    }

    @Override
    public void update(ReminderRule rule) {
        ruleMapper.update(ReminderRuleConverter.toDO(rule));
    }

    @Override
    public void deleteById(Long id) {
        ruleMapper.deleteById(id);
    }

    @Override
    public List<ReminderRule> selectEnabled() {
        return ruleMapper.selectEnabled().stream().map(ReminderRuleConverter::toEntity).toList();
    }

    @Override
    public List<ReminderRule> selectActiveBySourceAndMembers(String source, Collection<Long> familyMemberIds) {
        return ruleMapper.selectActiveBySourceAndMembers(source, familyMemberIds)
                .stream().map(ReminderRuleConverter::toEntity).toList();
    }
}
