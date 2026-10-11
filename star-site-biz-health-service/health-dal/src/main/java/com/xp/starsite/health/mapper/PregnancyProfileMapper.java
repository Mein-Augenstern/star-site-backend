package com.xp.starsite.health.mapper;

import com.xp.starsite.health.entity.PregnancyProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 孕期档案数据访问（DAL）：pregnancy_profile，每家庭成员至多一条，逻辑删。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface PregnancyProfileMapper {

    /** 按家庭成员取档案（一人一条；无档案返回 null） */
    PregnancyProfile selectByFamilyMemberId(@Param("familyMemberId") Long familyMemberId);

    /** 新建档案 */
    int insert(PregnancyProfile profile);

    /** 更新档案（按 id，更新 LMP/身高/孕前体重业务列） */
    int update(PregnancyProfile profile);
}
