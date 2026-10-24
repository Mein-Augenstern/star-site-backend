package com.xp.starsite.finance.repository.impl;

import com.xp.starsite.finance.converter.FinanceAccountConverter;
import com.xp.starsite.finance.dal.dataobject.FinanceAccountDO;
import com.xp.starsite.finance.entity.FinanceAccount;
import com.xp.starsite.finance.mapper.FinanceAccountMapper;
import com.xp.starsite.finance.repository.FinanceAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 资金账户仓储实现：调用 dal 的 {@link FinanceAccountMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class FinanceAccountRepositoryImpl implements FinanceAccountRepository {

    private final FinanceAccountMapper accountMapper;

    @Override
    public FinanceAccount save(FinanceAccount account) {
        FinanceAccountDO row = FinanceAccountConverter.toDO(account);
        accountMapper.insert(row);
        account.setId(row.getId());
        return account;
    }

    @Override
    public Optional<FinanceAccount> findById(Long id) {
        return Optional.ofNullable(accountMapper.selectById(id)).map(FinanceAccountConverter::toEntity);
    }

    @Override
    public void update(FinanceAccount account) {
        accountMapper.update(FinanceAccountConverter.toDO(account));
    }

    @Override
    public boolean updateBalanceDelta(Long id, BigDecimal delta) {
        return accountMapper.updateBalanceDelta(id, delta) > 0;
    }

    @Override
    public void markDeleted(Long id) {
        accountMapper.markDeleted(id);
    }

    @Override
    public List<FinanceAccount> listByOwner(Long ownerMemberId) {
        return accountMapper.selectByOwner(ownerMemberId)
                .stream().map(FinanceAccountConverter::toEntity).toList();
    }

    @Override
    public List<Long> listDistinctOwnerIds() {
        return accountMapper.selectDistinctOwnerIds();
    }
}
