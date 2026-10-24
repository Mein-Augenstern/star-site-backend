package com.xp.starsite.member.repository.impl;

import com.xp.starsite.member.converter.MemberIdentityConverter;
import com.xp.starsite.member.dal.dataobject.MemberIdentityDO;
import com.xp.starsite.member.entity.MemberIdentity;
import com.xp.starsite.member.mapper.MemberIdentityMapper;
import com.xp.starsite.member.repository.MemberIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 登录身份仓储实现：调用 dal 的 {@link MemberIdentityMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class MemberIdentityRepositoryImpl implements MemberIdentityRepository {

    private final MemberIdentityMapper identityMapper;

    @Override
    public MemberIdentity save(MemberIdentity identity) {
        MemberIdentityDO row = MemberIdentityConverter.toDO(identity);
        identityMapper.insert(row);
        identity.setId(row.getId());
        return identity;
    }

    @Override
    public Optional<MemberIdentity> findById(Long id) {
        return Optional.ofNullable(identityMapper.selectById(id)).map(MemberIdentityConverter::toEntity);
    }

    @Override
    public Optional<MemberIdentity> findByIdentity(String identityType, String identifier) {
        return Optional.ofNullable(identityMapper.selectByIdentity(identityType, identifier))
                .map(MemberIdentityConverter::toEntity);
    }

    @Override
    public Optional<MemberIdentity> findByUnionid(String identityType, String unionid) {
        return Optional.ofNullable(identityMapper.selectByUnionid(identityType, unionid))
                .map(MemberIdentityConverter::toEntity);
    }

    @Override
    public long countByIdentity(String identityType, String identifier) {
        return identityMapper.countByIdentity(identityType, identifier);
    }

    @Override
    public List<MemberIdentity> findByMemberId(Long memberId) {
        return identityMapper.selectByMemberId(memberId)
                .stream().map(MemberIdentityConverter::toEntity).toList();
    }

    @Override
    public long countByMemberId(Long memberId) {
        return identityMapper.countByMemberId(memberId);
    }

    @Override
    public void update(MemberIdentity identity) {
        identityMapper.update(MemberIdentityConverter.toDO(identity));
    }

    @Override
    public void markDeleted(Long id) {
        identityMapper.markDeleted(id);
    }
}
