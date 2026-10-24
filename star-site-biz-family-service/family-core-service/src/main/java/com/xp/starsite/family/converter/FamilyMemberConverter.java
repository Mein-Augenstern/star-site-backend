package com.xp.starsite.family.converter;

import com.xp.starsite.family.dal.dataobject.FamilyMemberDO;
import com.xp.starsite.family.entity.FamilyMember;
import org.springframework.beans.BeanUtils;

/**
 * 家庭成员模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class FamilyMemberConverter {

    private FamilyMemberConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static FamilyMemberDO toDO(FamilyMember member) {
        FamilyMemberDO row = new FamilyMemberDO();
        row.setId(member.getId());
        row.setFamilyId(member.getFamilyId());
        row.setName(member.getName());
        row.setRelation(member.getRelation());
        row.setGender(member.getGender());
        row.setBirthDate(member.getBirthDate());
        row.setPhone(member.getPhone());
        row.setAvatarFileId(member.getAvatarFileId());
        row.setLinkedMemberId(member.getLinkedMemberId());
        row.setRemark(member.getRemark());
        row.setDeleted(member.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static FamilyMember toEntity(FamilyMemberDO row) {
        FamilyMember member = new FamilyMember();
        BeanUtils.copyProperties(row, member);
        return member;
    }
}
