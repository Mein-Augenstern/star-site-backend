package com.xp.starsite.insurance.mapper;

import com.xp.starsite.insurance.dal.dataobject.InsurancePolicyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 保单数据访问（DAL）：insurance_policy，逻辑删。
 * SQL 全部收敛在 mapper XML，业务层只见业务语义方法。
 * 逻辑删：deleted 字段，查询一律带 deleted=0，删除为 UPDATE 置位。
 *
 * @author muyi
 * @since 2026-10
 */
@Mapper
public interface InsurancePolicyMapper {

    /** 新建保单（雪花 id 由业务层 insert 前赋值，事务消息载荷需要） */
    int insert(InsurancePolicyDO policy);

    /** 按 id 取保单（含逻辑删过滤，不存在返回 null） */
    InsurancePolicyDO selectById(@Param("id") Long id);

    /** 更新保单（按 id，更新业务列——编辑保单全量业务字段语义） */
    int update(InsurancePolicyDO policy);

    /** 逻辑删：deleted 置 1 */
    int markDeleted(@Param("id") Long id);

    /** 保单分页：限定持有账号（必填）+ 可选被保人/险种/状态，按 id 倒序 */
    List<InsurancePolicyDO> selectPage(@Param("ownerMemberId") Long ownerMemberId,
                                     @Param("insuredMemberId") Long insuredMemberId,
                                     @Param("category") String category,
                                     @Param("status") Integer status,
                                     @Param("offset") long offset,
                                     @Param("limit") int limit);

    /** 保单分页计数（与 selectPage 同条件） */
    long countByOwner(@Param("ownerMemberId") Long ownerMemberId,
                      @Param("insuredMemberId") Long insuredMemberId,
                      @Param("category") String category,
                      @Param("status") Integer status);

    /** 某账号全部有效保单（保障地图：状态=1 有效） */
    List<InsurancePolicyDO> selectActiveByOwner(@Param("ownerMemberId") Long ownerMemberId);
}
