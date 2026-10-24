package com.xp.starsite.health.repository.impl;

import com.xp.starsite.health.converter.VisitConverter;
import com.xp.starsite.health.dal.dataobject.VisitRecordDO;
import com.xp.starsite.health.mapper.VisitRecordMapper;
import com.xp.starsite.health.entity.VisitRecord;
import com.xp.starsite.health.repository.VisitRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 就医记录仓储实现：调用 dal 的 {@link VisitRecordMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class VisitRecordRepositoryImpl implements VisitRecordRepository {

    private final VisitRecordMapper visitMapper;

    @Override
    public VisitRecord save(VisitRecord visit) {
        VisitRecordDO row = VisitConverter.toDO(visit);
        visitMapper.insert(row);
        visit.setId(row.getId());
        return visit;
    }

    @Override
    public Optional<VisitRecord> findById(Long id) {
        return Optional.ofNullable(visitMapper.selectById(id)).map(VisitConverter::toEntity);
    }

    @Override
    public List<VisitRecord> pageByScope(Collection<Long> scopeIds, long offset, int limit) {
        return visitMapper.selectPage(scopeIds, offset, limit)
                .stream().map(VisitConverter::toEntity).toList();
    }

    @Override
    public long countByScope(Collection<Long> scopeIds) {
        return visitMapper.countByScope(scopeIds);
    }

    @Override
    public void markDeleted(Long id) {
        visitMapper.markDeleted(id);
    }
}
