package com.xp.starsite.finance.repository;

import com.xp.starsite.finance.entity.FinanceGoal;

import java.util.List;
import java.util.Optional;

/**
 * 家庭目标仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface FinanceGoalRepository {

    /** 保存（新增；雪花 id 应用侧先赋值） */
    FinanceGoal save(FinanceGoal goal);

    /** 按 id 取目标（含逻辑删过滤） */
    Optional<FinanceGoal> findById(Long id);

    /** 更新目标（业务列：名称/目标额/已存/截止日/备注） */
    void update(FinanceGoal goal);

    /** 逻辑删 */
    void markDeleted(Long id);

    /** 某账号全部目标（按截止日升序） */
    List<FinanceGoal> listByOwner(Long ownerMemberId);
}
