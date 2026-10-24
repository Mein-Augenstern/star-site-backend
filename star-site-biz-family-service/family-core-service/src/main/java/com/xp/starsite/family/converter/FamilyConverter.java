package com.xp.starsite.family.converter;

import com.xp.starsite.family.dal.dataobject.FamilyDO;
import com.xp.starsite.family.entity.Family;
import org.springframework.beans.BeanUtils;

/**
 * 家庭模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class FamilyConverter {

    private FamilyConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static FamilyDO toDO(Family family) {
        FamilyDO row = new FamilyDO();
        row.setId(family.getId());
        row.setName(family.getName());
        row.setOwnerMemberId(family.getOwnerMemberId());
        row.setRemark(family.getRemark());
        row.setDeleted(family.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static Family toEntity(FamilyDO row) {
        Family family = new Family();
        BeanUtils.copyProperties(row, family);
        return family;
    }
}
