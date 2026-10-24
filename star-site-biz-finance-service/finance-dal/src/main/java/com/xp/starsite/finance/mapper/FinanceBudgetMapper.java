package com.xp.starsite.finance.mapper;

import com.xp.starsite.finance.dal.dataobject.FinanceBudgetDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 月度预算数据访问（DAL）：finance_budget，逻辑删。
 * 「分类 × 月份」一人一条：save 语义为 selectByOwnerCategoryPeriod 命中则更新、未命中新建。
 *
 * @author muyi
 * @since 2026-10
 */
@Mapper
public interface FinanceBudgetMapper {

    /** 新建预算（雪花 id 由业务层 insert 前赋值） */
    int insert(FinanceBudgetDO budget);

    /** 按 id 取预算（含逻辑删过滤，不存在返回 null） */
    FinanceBudgetDO selectById(@Param("id") Long id);

    /** 「账号 × 分类 × 月份」唯一预算（upsert 判存用，XML LIMIT 1） */
    FinanceBudgetDO selectByOwnerCategoryPeriod(@Param("ownerMemberId") Long ownerMemberId,
                                              @Param("category") String category,
                                              @Param("period") String period);

    /** 更新预算金额（按 id，只更新业务变更列） */
    int update(FinanceBudgetDO budget);

    /** 逻辑删：deleted 置 1 */
    int markDeleted(@Param("id") Long id);

    /** 某账号某月份全部预算（按金额倒序展示） */
    List<FinanceBudgetDO> selectByOwnerPeriod(@Param("ownerMemberId") Long ownerMemberId,
                                            @Param("period") String period);
}
