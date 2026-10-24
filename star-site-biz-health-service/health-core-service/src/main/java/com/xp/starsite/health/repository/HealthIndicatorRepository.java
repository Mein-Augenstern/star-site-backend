package com.xp.starsite.health.repository;

import com.xp.starsite.health.entity.HealthIndicator;

import java.util.List;

/**
 * 体检指标仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 * health_indicator 无逻辑删，随报告重录物理清除。
 *
 * @author muyi
 * @since 2026-10
 */
public interface HealthIndicatorRepository {

    /** 保存单条指标（报告重录时由业务层循环调用） */
    HealthIndicator save(HealthIndicator indicator);

    /** 报告详情的指标全集（按 sortNo,id 升序） */
    List<HealthIndicator> findByReportId(Long reportId);

    /** 清空报告旧指标（重录覆盖语义） */
    void deleteByReportId(Long reportId);

    /** 指标名模糊命中的报告 id 集合（供报告搜索） */
    List<Long> findReportIdsByKeywordLike(String keyword);

    /** 某成员某指标历年趋势（按报告年份升序） */
    List<HealthIndicator> trendByItem(Long familyMemberId, String itemName);
}
