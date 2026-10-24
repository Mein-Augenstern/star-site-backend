package com.xp.starsite.finance.repository;

import com.xp.starsite.finance.entity.FinanceAccount;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 资金账户仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface FinanceAccountRepository {

    /** 保存（新增；雪花 id 应用侧先赋值） */
    FinanceAccount save(FinanceAccount account);

    /** 按 id 取账户（含逻辑删过滤） */
    Optional<FinanceAccount> findById(Long id);

    /** 编辑账户资料（只更新业务变更列：名称/类型/机构/备注，不动余额——余额只能经流水变更） */
    void update(FinanceAccount account);

    /**
     * 余额原子增量（一锁二判三更新，delta 可正可负）。
     *
     * @return false = 账户不存在/已删（调用方须判存报错）
     */
    boolean updateBalanceDelta(Long id, BigDecimal delta);

    /** 逻辑删（注销账户，余额须已为 0 由业务层校验） */
    void markDeleted(Long id);

    /** 某账号全部账户（按 id 升序，建户顺序展示） */
    List<FinanceAccount> listByOwner(Long ownerMemberId);

    /** 全部有账户的 owner id 去重集合（每日净资产快照调度用） */
    List<Long> listDistinctOwnerIds();
}
