package com.xp.starsite.health.repository;

import com.xp.starsite.health.entity.HealthReport;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 体检报告仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface HealthReportRepository {

    /** 保存（新增；回填自增 id） */
    HealthReport save(HealthReport report);

    /** 按 id 取（含逻辑删过滤） */
    Optional<HealthReport> findById(Long id);

    /** 分页：限定成员范围 + 可选年份，按体检日期倒序 */
    List<HealthReport> pageByScope(Collection<Long> scopeIds, Integer year, long offset, int limit);

    /** 分页计数（与 pageByScope 同条件） */
    long countByScope(Collection<Long> scopeIds, Integer year);

    /** 关键词搜索：命中标题/医院/总结，或 id 命中指标名命中的报告集合 */
    List<HealthReport> search(Collection<Long> scopeIds, String keyword, Collection<Long> reportIds);

    /** 逻辑删 */
    void markDeleted(Long id);

    /** AI 解读结果回填（解读文本 + 解读时间；异步解读完成后调用） */
    void updateInterpretation(Long id, String interpretation, LocalDateTime interpretedAt);
}
