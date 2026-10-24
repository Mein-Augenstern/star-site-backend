package com.xp.starsite.recheck.repository;

import com.xp.starsite.recheck.entity.RecheckPlan;

import java.util.List;
import java.util.Optional;

/**
 * 复查计划仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface RecheckPlanRepository {

    /** 保存（新增；雪花 id 应用侧先赋值） */
    RecheckPlan save(RecheckPlan plan);

    /** 按 id 取（含逻辑删过滤） */
    Optional<RecheckPlan> findById(Long id);

    /** 更新计划（状态流转与闭环回填语义） */
    void update(RecheckPlan plan);

    /** 逻辑删 */
    void markDeleted(Long id);

    /** 计划列表：限定归属账号（必填）+ 可选成员/状态，按 id 倒序（家庭量级小，不分页） */
    List<RecheckPlan> listByOwner(Long ownerMemberId, Long familyMemberId, Integer status);
}
