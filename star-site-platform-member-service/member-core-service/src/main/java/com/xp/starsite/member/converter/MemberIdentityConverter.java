package com.xp.starsite.member.converter;

import com.xp.starsite.member.dal.dataobject.MemberIdentityDO;
import com.xp.starsite.member.entity.MemberIdentity;
import org.springframework.beans.BeanUtils;

/**
 * 登录身份模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class MemberIdentityConverter {

    private MemberIdentityConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static MemberIdentityDO toDO(MemberIdentity identity) {
        MemberIdentityDO row = new MemberIdentityDO();
        row.setId(identity.getId());
        row.setMemberId(identity.getMemberId());
        row.setIdentityType(identity.getIdentityType());
        row.setIdentifier(identity.getIdentifier());
        row.setCredential(identity.getCredential());
        row.setUnionid(identity.getUnionid());
        row.setDeleted(identity.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static MemberIdentity toEntity(MemberIdentityDO row) {
        MemberIdentity identity = new MemberIdentity();
        BeanUtils.copyProperties(row, identity);
        return identity;
    }
}
