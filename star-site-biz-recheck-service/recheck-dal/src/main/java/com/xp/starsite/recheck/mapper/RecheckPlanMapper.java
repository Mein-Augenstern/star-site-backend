package com.xp.starsite.recheck.mapper;

import com.xp.starsite.recheck.dal.dataobject.RecheckPlanDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 复查计划数据访问（DAL）：recheck_plan，逻辑删。
 * SQL 全部收敛在 mapper XML，业务层只见业务语义方法。
 * 逻辑删：deleted 字段，查询一律带 deleted=0，删除为 UPDATE 置位。
 *
 * @author muyi
 * @since 2026-10
 */
@Mapper
public interface RecheckPlanMapper {

    /** 新建计划（雪花 id 由业务层 insert 前赋值：reminder 规则 bizId 需要） */
    int insert(RecheckPlanDO plan);

    /** 按 id 取计划（含逻辑删过滤，不存在返回 null） */
    RecheckPlanDO selectById(@Param("id") Long id);

    /** 更新计划（按 id，更新业务列——状态流转与闭环回填语义） */
    int update(RecheckPlanDO plan);

    /** 逻辑删：deleted 置 1 */
    int markDeleted(@Param("id") Long id);

    /** 计划列表：限定归属账号（必填）+ 可选成员/状态，按 id 倒序（家庭量级小，不分页） */
    List<RecheckPlanDO> selectByOwner(@Param("ownerMemberId") Long ownerMemberId,
                                      @Param("familyMemberId") Long familyMemberId,
                                      @Param("status") Integer status);
}
