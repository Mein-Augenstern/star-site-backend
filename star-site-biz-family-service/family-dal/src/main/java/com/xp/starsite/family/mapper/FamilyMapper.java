package com.xp.starsite.family.mapper;

import com.xp.starsite.family.dal.dataobject.FamilyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * family 家庭表数据访问（DAL）：SQL 全部收敛在 mapper XML，业务层只见业务语义方法。
 * 逻辑删：family.deleted，查询一律带 deleted=0。
 * 主键自增：INSERT 不含 id（useGeneratedKeys 回填），created_at/updated_at 由 DB 维护。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface FamilyMapper {

    /** 新建家庭 */
    int insert(FamilyDO family);

    /** 按 id 取家庭（含逻辑删过滤，不存在返回 null） */
    FamilyDO selectById(@Param("id") Long id);

    /** 按顶梁柱账号取家庭（一账号一家庭；至多一条，不存在返回 null） */
    FamilyDO selectByOwner(@Param("ownerMemberId") Long ownerMemberId);

    /** 顶梁柱建家计数（防重复创建；与 selectByOwner 同条件 deleted=0） */
    long countByOwner(@Param("ownerMemberId") Long ownerMemberId);
}
