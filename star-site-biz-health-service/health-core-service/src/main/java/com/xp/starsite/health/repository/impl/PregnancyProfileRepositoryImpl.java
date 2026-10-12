package com.xp.starsite.health.repository.impl;

import com.xp.starsite.health.converter.PregnancyConverter;
import com.xp.starsite.health.dal.dataobject.PregnancyProfileDO;
import com.xp.starsite.health.mapper.PregnancyProfileMapper;
import com.xp.starsite.health.entity.PregnancyProfile;
import com.xp.starsite.health.repository.PregnancyProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 孕期档案仓储实现：调用 dal 的 {@link PregnancyProfileMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class PregnancyProfileRepositoryImpl implements PregnancyProfileRepository {

    private final PregnancyProfileMapper mapper;

    @Override
    public Optional<PregnancyProfile> findByFamilyMemberId(Long familyMemberId) {
        return Optional.ofNullable(mapper.selectByFamilyMemberId(familyMemberId))
                .map(PregnancyConverter::toEntity);
    }

    @Override
    public PregnancyProfile save(PregnancyProfile profile) {
        PregnancyProfileDO row = PregnancyConverter.toDO(profile);
        if (row.getId() == null) {
            mapper.insert(row);
        } else {
            mapper.update(row);
        }
        profile.setId(row.getId());
        return profile;
    }
}
