package com.xp.starsite.health.repository;

import com.xp.starsite.health.entity.VisitRecord;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 就医记录仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface VisitRecordRepository {

    /** 保存（新增；回填自增 id） */
    VisitRecord save(VisitRecord visit);

    /** 按 id 取（含逻辑删过滤） */
    Optional<VisitRecord> findById(Long id);

    /** 分页：限定成员范围，按就诊日期倒序 */
    List<VisitRecord> pageByScope(Collection<Long> scopeIds, long offset, int limit);

    /** 分页计数（与 pageByScope 同条件） */
    long countByScope(Collection<Long> scopeIds);

    /** 逻辑删 */
    void markDeleted(Long id);
}
