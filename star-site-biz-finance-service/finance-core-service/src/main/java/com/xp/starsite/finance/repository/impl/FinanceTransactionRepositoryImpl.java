package com.xp.starsite.finance.repository.impl;

import com.xp.starsite.finance.converter.FinanceTransactionConverter;
import com.xp.starsite.finance.dal.dataobject.FinanceTransactionDO;
import com.xp.starsite.finance.entity.FinanceTransaction;
import com.xp.starsite.finance.mapper.FinanceTransactionMapper;
import com.xp.starsite.finance.repository.FinanceTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 记账流水仓储实现：调用 dal 的 {@link FinanceTransactionMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class FinanceTransactionRepositoryImpl implements FinanceTransactionRepository {

    private final FinanceTransactionMapper transactionMapper;

    @Override
    public FinanceTransaction save(FinanceTransaction transaction) {
        FinanceTransactionDO row = FinanceTransactionConverter.toDO(transaction);
        transactionMapper.insert(row);
        transaction.setId(row.getId());
        return transaction;
    }

    @Override
    public Optional<FinanceTransaction> findById(Long id) {
        return Optional.ofNullable(transactionMapper.selectById(id)).map(FinanceTransactionConverter::toEntity);
    }

    @Override
    public void markDeleted(Long id) {
        transactionMapper.markDeleted(id);
    }

    @Override
    public List<FinanceTransaction> findByTransferGroupId(Long transferGroupId) {
        return transactionMapper.selectByTransferGroupId(transferGroupId)
                .stream().map(FinanceTransactionConverter::toEntity).toList();
    }

    @Override
    public List<FinanceTransaction> pageByOwner(Long ownerMemberId, Long accountId, Integer direction,
                                                LocalDateTime startTime, LocalDateTime endTime,
                                                long offset, int limit) {
        return transactionMapper
                .selectPage(ownerMemberId, accountId, direction, startTime, endTime, offset, limit)
                .stream().map(FinanceTransactionConverter::toEntity).toList();
    }

    @Override
    public long countByOwner(Long ownerMemberId, Long accountId, Integer direction,
                             LocalDateTime startTime, LocalDateTime endTime) {
        return transactionMapper.countByCondition(ownerMemberId, accountId, direction, startTime, endTime);
    }

    @Override
    public List<FinanceTransaction> listByDirectionAndPeriod(Long ownerMemberId, int direction,
                                                             LocalDateTime startTime, LocalDateTime endTime,
                                                             String category) {
        return transactionMapper.selectByDirectionAndPeriod(ownerMemberId, direction, startTime, endTime, category)
                .stream().map(FinanceTransactionConverter::toEntity).toList();
    }

    @Override
    public List<FinanceTransaction> listIncomeExpensePeriod(Long ownerMemberId,
                                                            LocalDateTime startTime, LocalDateTime endTime) {
        return transactionMapper.selectIncomeExpensePeriod(ownerMemberId, startTime, endTime)
                .stream().map(FinanceTransactionConverter::toEntity).toList();
    }
}
