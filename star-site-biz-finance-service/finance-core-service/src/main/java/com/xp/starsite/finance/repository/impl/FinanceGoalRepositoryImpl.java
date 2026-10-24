package com.xp.starsite.finance.repository.impl;

import com.xp.starsite.finance.converter.FinanceGoalConverter;
import com.xp.starsite.finance.dal.dataobject.FinanceGoalDO;
import com.xp.starsite.finance.entity.FinanceGoal;
import com.xp.starsite.finance.mapper.FinanceGoalMapper;
import com.xp.starsite.finance.repository.FinanceGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 家庭目标仓储实现：调用 dal 的 {@link FinanceGoalMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class FinanceGoalRepositoryImpl implements FinanceGoalRepository {

    private final FinanceGoalMapper goalMapper;

    @Override
    public FinanceGoal save(FinanceGoal goal) {
        FinanceGoalDO row = FinanceGoalConverter.toDO(goal);
        goalMapper.insert(row);
        goal.setId(row.getId());
        return goal;
    }

    @Override
    public Optional<FinanceGoal> findById(Long id) {
        return Optional.ofNullable(goalMapper.selectById(id)).map(FinanceGoalConverter::toEntity);
    }

    @Override
    public void update(FinanceGoal goal) {
        goalMapper.update(FinanceGoalConverter.toDO(goal));
    }

    @Override
    public void markDeleted(Long id) {
        goalMapper.markDeleted(id);
    }

    @Override
    public List<FinanceGoal> listByOwner(Long ownerMemberId) {
        return goalMapper.selectByOwner(ownerMemberId)
                .stream().map(FinanceGoalConverter::toEntity).toList();
    }
}
