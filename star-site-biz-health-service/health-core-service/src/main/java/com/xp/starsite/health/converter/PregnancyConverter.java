package com.xp.starsite.health.converter;

import com.xp.starsite.health.dal.dataobject.PregnancyProfileDO;
import com.xp.starsite.health.entity.PregnancyProfile;
import org.springframework.beans.BeanUtils;

/**
 * 孕期档案模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 *
 * @author muyi
 * @since 2026-10
 */
public final class PregnancyConverter {

    private PregnancyConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static PregnancyProfileDO toDO(PregnancyProfile profile) {
        PregnancyProfileDO row = new PregnancyProfileDO();
        row.setId(profile.getId());
        row.setFamilyMemberId(profile.getFamilyMemberId());
        row.setLmpDate(profile.getLmpDate());
        row.setHeightCm(profile.getHeightCm());
        row.setPrePregnancyWeight(profile.getPrePregnancyWeight());
        row.setEnded(profile.getEnded());
        row.setRemark(profile.getRemark());
        row.setDeleted(profile.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static PregnancyProfile toEntity(PregnancyProfileDO row) {
        PregnancyProfile profile = new PregnancyProfile();
        BeanUtils.copyProperties(row, profile);
        return profile;
    }
}
