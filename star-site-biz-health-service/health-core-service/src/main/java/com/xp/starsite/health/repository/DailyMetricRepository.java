package com.xp.starsite.health.repository;

import com.xp.starsite.health.entity.DailyMetric;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 日常健康指标仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface DailyMetricRepository {

    /** 保存（新增；回填自增 id） */
    DailyMetric save(DailyMetric metric);

    /** 按 id 取（含逻辑删过滤，不存在返回 empty） */
    Optional<DailyMetric> findById(Long id);

    /** 逻辑删 */
    void markDeleted(Long id);

    /** 分页：限定成员范围 + 可选指标类型，按测量日期倒序 */
    List<DailyMetric> pageByScope(Collection<Long> scopeIds, String metricType, long offset, int limit);

    /** 分页计数（与 pageByScope 同条件） */
    long countByScope(Collection<Long> scopeIds, String metricType);

    /** 某成员某指标趋势（按测量日期升序，孕期体重曲线等） */
    List<DailyMetric> selectTrend(Long familyMemberId, String metricType);
}
