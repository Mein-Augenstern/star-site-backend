package com.xp.starsite.finance.mapper;

import com.xp.starsite.finance.entity.FinanceAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资金账户数据访问（DAL）：finance_account，逻辑删。
 * 余额由流水增量维护——更新余额走显式 {@link #updateBalance}（不整行 update，避免覆盖并发流水）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface FinanceAccountMapper {

    /** 新建账户（雪花 id 由业务层 insert 前赋值；余额=期初余额一并落库） */
    int insert(FinanceAccount account);

    /** 按 id 取账户（含逻辑删过滤，不存在返回 null） */
    FinanceAccount selectById(@Param("id") Long id);

    /** 编辑账户资料（按 id，只更新业务变更列：名称/类型/机构/备注，不动余额） */
    int update(FinanceAccount account);

    /** 余额增量维护（流水/导入/回滚后由业务层算好新余额写入） */
    int updateBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);

    /** 逻辑删：deleted 置 1（注销账户，余额须已为 0 由业务层校验） */
    int markDeleted(@Param("id") Long id);

    /** 某账号全部账户（按 id 升序，建户顺序展示） */
    List<FinanceAccount> selectByOwner(@Param("ownerMemberId") Long ownerMemberId);

    /** 全部有账户的 owner id 去重集合（每日净资产快照调度用） */
    List<Long> selectDistinctOwnerIds();
}
