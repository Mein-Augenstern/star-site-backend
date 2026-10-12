package com.xp.starsite.member.converter;

import com.xp.starsite.member.dal.dataobject.MemberDO;
import com.xp.starsite.member.entity.Member;
import org.springframework.beans.BeanUtils;

/**
 * 会员模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class MemberConverter {

    private MemberConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static MemberDO toDO(Member member) {
        MemberDO row = new MemberDO();
        row.setId(member.getId());
        row.setUsername(member.getUsername());
        row.setPassword(member.getPassword());
        row.setNickname(member.getNickname());
        row.setAvatar(member.getAvatar());
        row.setEmail(member.getEmail());
        row.setPhone(member.getPhone());
        row.setGender(member.getGender());
        row.setBio(member.getBio());
        row.setStatus(member.getStatus());
        row.setDeleted(member.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static Member toEntity(MemberDO row) {
        Member member = new Member();
        BeanUtils.copyProperties(row, member);
        return member;
    }
}
