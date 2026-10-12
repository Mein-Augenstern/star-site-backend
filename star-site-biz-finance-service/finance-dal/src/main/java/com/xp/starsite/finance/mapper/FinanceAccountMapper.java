package com.xp.starsite.finance.mapper;

import com.xp.starsite.finance.dal.dataobject.FinanceAccountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资金账户数据访问（DAL）：finance_account，逻辑删。
 * 余额维护走 {@link #updateBalanceDelta} 原子增量（一锁二判三更新：
 * UPDATE 行锁锁账户 → WHERE deleted=0 判存在 → balance=balance+delta 原子更新），
 * 并发流水/导入下不丢更新；严禁业务层读-算-写覆盖余额。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface FinanceAccountMapper {

    /** 新建账户（雪花 id 由业务层 insert 前赋值；余额=期初余额一并落库） */
    int insert(FinanceAccountDO account);

    /** 按 id 取账户（含逻辑删过滤，不存在返回 null） */
    FinanceAccountDO selectById(@Param("id") Long id);

    /** 编辑账户资料（按 id，只更新业务变更列：名称/类型/机构/备注，不动余额） */
    int update(FinanceAccountDO account);

    /**
     * 余额原子增量（delta 可正可负）。
     *
     * @return 影响行数：0 = 账户不存在/已删（调用方须判存报错）
     */
    int updateBalanceDelta(@Param("id") Long id, @Param("delta") BigDecimal delta);

    /** 逻辑删：deleted 置 1（注销账户，余额须已为 0 由业务层校验） */
    int markDeleted(@Param("id") Long id);

    /** 某账号全部账户（按 id 升序，建户顺序展示） */
    List<FinanceAccountDO> selectByOwner(@Param("ownerMemberId") Long ownerMemberId);

    /** 全部有账户的 owner id 去重集合（每日净资产快照调度用） */
    List<Long> selectDistinctOwnerIds();
}
