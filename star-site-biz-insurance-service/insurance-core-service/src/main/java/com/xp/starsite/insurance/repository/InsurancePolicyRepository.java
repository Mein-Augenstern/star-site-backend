package com.xp.starsite.insurance.repository;

import com.xp.starsite.insurance.entity.InsurancePolicy;

import java.util.List;
import java.util.Optional;

/**
 * 保单仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface InsurancePolicyRepository {

    /** 保存（新增；雪花 id 应用侧先赋值） */
    InsurancePolicy save(InsurancePolicy policy);

    /** 按 id 取（含逻辑删过滤） */
    Optional<InsurancePolicy> findById(Long id);

    /** 更新保单（编辑全量业务字段语义） */
    void update(InsurancePolicy policy);

    /** 逻辑删 */
    void markDeleted(Long id);

    /** 保单分页：限定持有账号（必填）+ 可选被保人/险种/状态，按 id 倒序 */
    List<InsurancePolicy> pageByOwner(Long ownerMemberId, Long insuredMemberId, String category,
                                      Integer status, long offset, int limit);

    /** 保单分页计数（与 pageByOwner 同条件） */
    long countByOwner(Long ownerMemberId, Long insuredMemberId, String category, Integer status);

    /** 某账号全部有效保单（保障地图：状态=1 有效） */
    List<InsurancePolicy> selectActiveByOwner(Long ownerMemberId);
}
