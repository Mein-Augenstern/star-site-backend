package com.xp.starsite.finance.repository;

import com.xp.starsite.finance.entity.FinanceBudget;

import java.util.List;
import java.util.Optional;

/**
 * 月度预算仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface FinanceBudgetRepository {

    /** 保存（新增；雪花 id 应用侧先赋值） */
    FinanceBudget save(FinanceBudget budget);

    /** 按 id 取预算（含逻辑删过滤） */
    Optional<FinanceBudget> findById(Long id);

    /** 「账号 × 分类 × 月份」唯一预算（upsert 判存用） */
    Optional<FinanceBudget> findByOwnerCategoryPeriod(Long ownerMemberId, String category, String period);

    /** 更新预算金额（保存语义只变更金额） */
    void update(FinanceBudget budget);

    /** 逻辑删 */
    void markDeleted(Long id);

    /** 某账号某月份全部预算（按金额倒序展示） */
    List<FinanceBudget> listByOwnerPeriod(Long ownerMemberId, String period);
}
