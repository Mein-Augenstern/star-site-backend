package com.xp.starsite.insurance.repository.impl;

import com.xp.starsite.insurance.converter.InsurancePolicyConverter;
import com.xp.starsite.insurance.dal.dataobject.InsurancePolicyDO;
import com.xp.starsite.insurance.entity.InsurancePolicy;
import com.xp.starsite.insurance.mapper.InsurancePolicyMapper;
import com.xp.starsite.insurance.repository.InsurancePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 保单仓储实现：调用 dal 的 {@link InsurancePolicyMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class InsurancePolicyRepositoryImpl implements InsurancePolicyRepository {

    private final InsurancePolicyMapper policyMapper;

    @Override
    public InsurancePolicy save(InsurancePolicy policy) {
        InsurancePolicyDO row = InsurancePolicyConverter.toDO(policy);
        policyMapper.insert(row);
        policy.setId(row.getId());
        return policy;
    }

    @Override
    public Optional<InsurancePolicy> findById(Long id) {
        return Optional.ofNullable(policyMapper.selectById(id)).map(InsurancePolicyConverter::toEntity);
    }

    @Override
    public void update(InsurancePolicy policy) {
        policyMapper.update(InsurancePolicyConverter.toDO(policy));
    }

    @Override
    public void markDeleted(Long id) {
        policyMapper.markDeleted(id);
    }

    @Override
    public List<InsurancePolicy> pageByOwner(Long ownerMemberId, Long insuredMemberId, String category,
                                             Integer status, long offset, int limit) {
        return policyMapper.selectPage(ownerMemberId, insuredMemberId, category, status, offset, limit)
                .stream().map(InsurancePolicyConverter::toEntity).toList();
    }

    @Override
    public long countByOwner(Long ownerMemberId, Long insuredMemberId, String category, Integer status) {
        return policyMapper.countByOwner(ownerMemberId, insuredMemberId, category, status);
    }

    @Override
    public List<InsurancePolicy> selectActiveByOwner(Long ownerMemberId) {
        return policyMapper.selectActiveByOwner(ownerMemberId)
                .stream().map(InsurancePolicyConverter::toEntity).toList();
    }
}
