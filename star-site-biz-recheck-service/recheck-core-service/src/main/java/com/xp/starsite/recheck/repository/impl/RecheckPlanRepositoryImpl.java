package com.xp.starsite.recheck.repository.impl;

import com.xp.starsite.recheck.converter.RecheckPlanConverter;
import com.xp.starsite.recheck.dal.dataobject.RecheckPlanDO;
import com.xp.starsite.recheck.entity.RecheckPlan;
import com.xp.starsite.recheck.mapper.RecheckPlanMapper;
import com.xp.starsite.recheck.repository.RecheckPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 复查计划仓储实现：调用 dal 的 {@link RecheckPlanMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class RecheckPlanRepositoryImpl implements RecheckPlanRepository {

    private final RecheckPlanMapper planMapper;

    @Override
    public RecheckPlan save(RecheckPlan plan) {
        RecheckPlanDO row = RecheckPlanConverter.toDO(plan);
        planMapper.insert(row);
        return plan;
    }

    @Override
    public Optional<RecheckPlan> findById(Long id) {
        return Optional.ofNullable(planMapper.selectById(id)).map(RecheckPlanConverter::toEntity);
    }

    @Override
    public void update(RecheckPlan plan) {
        planMapper.update(RecheckPlanConverter.toDO(plan));
    }

    @Override
    public void markDeleted(Long id) {
        planMapper.markDeleted(id);
    }

    @Override
    public List<RecheckPlan> listByOwner(Long ownerMemberId, Long familyMemberId, Integer status) {
        return planMapper.selectByOwner(ownerMemberId, familyMemberId, status)
                .stream().map(RecheckPlanConverter::toEntity).toList();
    }
}
