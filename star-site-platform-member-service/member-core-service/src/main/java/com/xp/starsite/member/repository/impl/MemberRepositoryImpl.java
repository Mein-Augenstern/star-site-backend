package com.xp.starsite.member.repository.impl;

import com.xp.starsite.member.converter.MemberConverter;
import com.xp.starsite.member.dal.dataobject.MemberDO;
import com.xp.starsite.member.entity.Member;
import com.xp.starsite.member.mapper.MemberMapper;
import com.xp.starsite.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 会员主表仓储实现：调用 dal 的 {@link MemberMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberMapper memberMapper;

    @Override
    public Member save(Member member) {
        MemberDO row = MemberConverter.toDO(member);
        memberMapper.insert(row);
        member.setId(row.getId());
        return member;
    }

    @Override
    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(memberMapper.selectById(id)).map(MemberConverter::toEntity);
    }

    @Override
    public Optional<Member> findByUsername(String username) {
        return Optional.ofNullable(memberMapper.selectByUsername(username)).map(MemberConverter::toEntity);
    }

    @Override
    public long countByUsername(String username) {
        return memberMapper.countByUsername(username);
    }

    @Override
    public void updateProfile(Member member) {
        memberMapper.updateProfile(MemberConverter.toDO(member));
    }

    @Override
    public void updatePassword(Long id, String password) {
        memberMapper.updatePassword(id, password);
    }
}
