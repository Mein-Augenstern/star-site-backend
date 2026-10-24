package com.xp.starsite.finance.repository.impl;

import com.xp.starsite.finance.converter.FinanceBudgetConverter;
import com.xp.starsite.finance.dal.dataobject.FinanceBudgetDO;
import com.xp.starsite.finance.entity.FinanceBudget;
import com.xp.starsite.finance.mapper.FinanceBudgetMapper;
import com.xp.starsite.finance.repository.FinanceBudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 月度预算仓储实现：调用 dal 的 {@link FinanceBudgetMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class FinanceBudgetRepositoryImpl implements FinanceBudgetRepository {

    private final FinanceBudgetMapper budgetMapper;

    @Override
    public FinanceBudget save(FinanceBudget budget) {
        FinanceBudgetDO row = FinanceBudgetConverter.toDO(budget);
        budgetMapper.insert(row);
        budget.setId(row.getId());
        return budget;
    }

    @Override
    public Optional<FinanceBudget> findById(Long id) {
        return Optional.ofNullable(budgetMapper.selectById(id)).map(FinanceBudgetConverter::toEntity);
    }

    @Override
    public Optional<FinanceBudget> findByOwnerCategoryPeriod(Long ownerMemberId, String category, String period) {
        return Optional.ofNullable(budgetMapper.selectByOwnerCategoryPeriod(ownerMemberId, category, period))
                .map(FinanceBudgetConverter::toEntity);
    }

    @Override
    public void update(FinanceBudget budget) {
        budgetMapper.update(FinanceBudgetConverter.toDO(budget));
    }

    @Override
    public void markDeleted(Long id) {
        budgetMapper.markDeleted(id);
    }

    @Override
    public List<FinanceBudget> listByOwnerPeriod(Long ownerMemberId, String period) {
        return budgetMapper.selectByOwnerPeriod(ownerMemberId, period)
                .stream().map(FinanceBudgetConverter::toEntity).toList();
    }
}
