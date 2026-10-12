package com.xp.starsite.finance.repository.impl;

import com.xp.starsite.finance.converter.FinanceNetWorthSnapshotConverter;
import com.xp.starsite.finance.dal.dataobject.FinanceNetWorthSnapshotDO;
import com.xp.starsite.finance.entity.FinanceNetWorthSnapshot;
import com.xp.starsite.finance.mapper.FinanceNetWorthSnapshotMapper;
import com.xp.starsite.finance.repository.FinanceNetWorthSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 净资产快照仓储实现：调用 dal 的 {@link FinanceNetWorthSnapshotMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class FinanceNetWorthSnapshotRepositoryImpl implements FinanceNetWorthSnapshotRepository {

    private final FinanceNetWorthSnapshotMapper snapshotMapper;

    @Override
    public FinanceNetWorthSnapshot save(FinanceNetWorthSnapshot snapshot) {
        FinanceNetWorthSnapshotDO row = FinanceNetWorthSnapshotConverter.toDO(snapshot);
        snapshotMapper.insert(row);
        snapshot.setId(row.getId());
        return snapshot;
    }

    @Override
    public Optional<FinanceNetWorthSnapshot> findByOwnerAndDate(Long ownerMemberId, LocalDate snapDate) {
        return Optional.ofNullable(snapshotMapper.selectByOwnerAndDate(ownerMemberId, snapDate))
                .map(FinanceNetWorthSnapshotConverter::toEntity);
    }

    @Override
    public void update(FinanceNetWorthSnapshot snapshot) {
        snapshotMapper.update(FinanceNetWorthSnapshotConverter.toDO(snapshot));
    }

    @Override
    public List<FinanceNetWorthSnapshot> listByOwner(Long ownerMemberId) {
        return snapshotMapper.selectByOwner(ownerMemberId)
                .stream().map(FinanceNetWorthSnapshotConverter::toEntity).toList();
    }
}
