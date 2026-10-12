package com.xp.starsite.finance.mapper;

import com.xp.starsite.finance.dal.dataobject.FinanceGoalDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 家庭目标数据访问（DAL）：finance_goal，逻辑删。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface FinanceGoalMapper {

    /** 新建目标（雪花 id 由业务层 insert 前赋值） */
    int insert(FinanceGoalDO goal);

    /** 按 id 取目标（含逻辑删过滤，不存在返回 null） */
    FinanceGoalDO selectById(@Param("id") Long id);

    /** 更新目标（按 id，更新业务列：名称/目标额/已存/截止日/备注） */
    int update(FinanceGoalDO goal);

    /** 逻辑删：deleted 置 1 */
    int markDeleted(@Param("id") Long id);

    /** 某账号全部目标（按截止日升序） */
    List<FinanceGoalDO> selectByOwner(@Param("ownerMemberId") Long ownerMemberId);
}
