package com.xp.starsite.reminder.repository.impl;

import com.xp.starsite.reminder.converter.ReminderOccurrenceConverter;
import com.xp.starsite.reminder.dal.dataobject.ReminderOccurrenceDO;
import com.xp.starsite.reminder.entity.ReminderOccurrence;
import com.xp.starsite.reminder.mapper.ReminderOccurrenceMapper;
import com.xp.starsite.reminder.repository.ReminderOccurrenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 提醒实例仓储实现：调用 dal 的 {@link ReminderOccurrenceMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class ReminderOccurrenceRepositoryImpl implements ReminderOccurrenceRepository {

    private final ReminderOccurrenceMapper occurrenceMapper;

    @Override
    public ReminderOccurrence save(ReminderOccurrence occurrence) {
        ReminderOccurrenceDO row = ReminderOccurrenceConverter.toDO(occurrence);
        occurrenceMapper.insert(row);
        occurrence.setId(row.getId());
        return occurrence;
    }

    @Override
    public Optional<ReminderOccurrence> findById(Long id) {
        return Optional.ofNullable(occurrenceMapper.selectById(id)).map(ReminderOccurrenceConverter::toEntity);
    }

    @Override
    public int countByRuleAndDueDate(Long ruleId, LocalDate dueDate) {
        return occurrenceMapper.countByRuleAndDueDate(ruleId, dueDate);
    }

    @Override
    public long countActiveByRuleId(Long ruleId) {
        return occurrenceMapper.countActiveByRuleId(ruleId);
    }

    @Override
    public void deleteActiveByRuleId(Long ruleId) {
        occurrenceMapper.deleteActiveByRuleId(ruleId);
    }

    @Override
    public List<ReminderOccurrence> selectDuePendingUpTo(LocalDate dueDate) {
        return occurrenceMapper.selectDuePendingUpTo(dueDate)
                .stream().map(ReminderOccurrenceConverter::toEntity).toList();
    }

    @Override
    public List<ReminderOccurrence> selectUpcomingByOwner(Long ownerMemberId, LocalDate dueBefore) {
        return occurrenceMapper.selectUpcomingByOwner(ownerMemberId, dueBefore)
                .stream().map(ReminderOccurrenceConverter::toEntity).toList();
    }

    @Override
    public void markNotified(Long id, LocalDateTime notifiedAt) {
        occurrenceMapper.markNotified(id, notifiedAt);
    }

    @Override
    public void updateStatus(Long id, int status) {
        occurrenceMapper.updateStatus(id, status);
    }
}
