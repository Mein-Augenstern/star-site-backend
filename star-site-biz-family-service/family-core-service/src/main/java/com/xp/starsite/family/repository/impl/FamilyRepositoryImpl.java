package com.xp.starsite.family.repository.impl;

import com.xp.starsite.family.converter.FamilyConverter;
import com.xp.starsite.family.dal.dataobject.FamilyDO;
import com.xp.starsite.family.entity.Family;
import com.xp.starsite.family.mapper.FamilyMapper;
import com.xp.starsite.family.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 家庭仓储实现：调用 dal 的 {@link FamilyMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class FamilyRepositoryImpl implements FamilyRepository {

    private final FamilyMapper familyMapper;

    @Override
    public Family save(Family family) {
        FamilyDO row = FamilyConverter.toDO(family);
        familyMapper.insert(row);
        family.setId(row.getId());
        return family;
    }

    @Override
    public Optional<Family> findById(Long id) {
        return Optional.ofNullable(familyMapper.selectById(id)).map(FamilyConverter::toEntity);
    }

    @Override
    public Optional<Family> findByOwner(Long ownerMemberId) {
        return Optional.ofNullable(familyMapper.selectByOwner(ownerMemberId)).map(FamilyConverter::toEntity);
    }

    @Override
    public long countByOwner(Long ownerMemberId) {
        return familyMapper.countByOwner(ownerMemberId);
    }
}
