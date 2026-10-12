package com.xp.starsite.family.repository.impl;

import com.xp.starsite.family.converter.FamilyMemberConverter;
import com.xp.starsite.family.dal.dataobject.FamilyMemberDO;
import com.xp.starsite.family.entity.FamilyMember;
import com.xp.starsite.family.mapper.FamilyMemberMapper;
import com.xp.starsite.family.repository.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 家庭成员档案仓储实现：调用 dal 的 {@link FamilyMemberMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class FamilyMemberRepositoryImpl implements FamilyMemberRepository {

    private final FamilyMemberMapper familyMemberMapper;

    @Override
    public FamilyMember save(FamilyMember member) {
        FamilyMemberDO row = FamilyMemberConverter.toDO(member);
        familyMemberMapper.insert(row);
        member.setId(row.getId());
        return member;
    }

    @Override
    public Optional<FamilyMember> findById(Long id) {
        return Optional.ofNullable(familyMemberMapper.selectById(id)).map(FamilyMemberConverter::toEntity);
    }

    @Override
    public List<FamilyMember> findByFamilyId(Long familyId) {
        return familyMemberMapper.selectByFamilyId(familyId)
                .stream().map(FamilyMemberConverter::toEntity).toList();
    }

    @Override
    public void update(FamilyMember member) {
        familyMemberMapper.update(FamilyMemberConverter.toDO(member));
    }

    @Override
    public void markDeleted(Long id) {
        familyMemberMapper.markDeleted(id);
    }
}
