package com.xp.starsite.family.mapper;

import com.xp.starsite.family.dal.dataobject.FamilyMemberDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * family_member 家庭成员档案表数据访问（DAL）：SQL 全部收敛在 mapper XML。
 * 逻辑删：family_member.deleted，查询一律带 deleted=0，删除为 UPDATE 置位。
 * 归属校验（FamilyOwnershipGuard → family.internal 接口）依赖 selectById 的 family_id 归属判断。
 *
 * @author muyi
 * @since 2026-10
 */
@Mapper
public interface FamilyMemberMapper {

    /** 新建成员档案（useGeneratedKeys 回填 id） */
    int insert(FamilyMemberDO member);

    /** 按 id 取成员（含逻辑删过滤，不存在返回 null） */
    FamilyMemberDO selectById(@Param("id") Long id);

    /** 某家庭全部成员（按 id 升序：建档顺序即列表顺序） */
    List<FamilyMemberDO> selectByFamilyId(@Param("familyId") Long familyId);

    /**
     * 成员档案编辑：仅更新非空字段（name/relation/gender/birthDate/phone/avatarFileId/
     * linkedMemberId/remark，与原 MyBatis-Plus updateById 的"null 字段不更新"语义一致）。
     */
    int update(FamilyMemberDO member);

    /** 逻辑删：deleted 置 1 */
    int markDeleted(@Param("id") Long id);
}
