package com.xp.starsite.health.repository.impl;

import com.xp.starsite.health.converter.MetricConverter;
import com.xp.starsite.health.dal.dataobject.DailyMetricDO;
import com.xp.starsite.health.mapper.DailyMetricMapper;
import com.xp.starsite.health.entity.DailyMetric;
import com.xp.starsite.health.repository.DailyMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 日常健康指标仓储实现：调用 dal 的 {@link DailyMetricMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class DailyMetricRepositoryImpl implements DailyMetricRepository {

    private final DailyMetricMapper metricMapper;

    @Override
    public DailyMetric save(DailyMetric metric) {
        DailyMetricDO row = MetricConverter.toDO(metric);
        metricMapper.insert(row);
        metric.setId(row.getId());
        return metric;
    }

    @Override
    public Optional<DailyMetric> findById(Long id) {
        return Optional.ofNullable(metricMapper.selectById(id)).map(MetricConverter::toEntity);
    }

    @Override
    public void markDeleted(Long id) {
        metricMapper.markDeleted(id);
    }

    @Override
    public List<DailyMetric> pageByScope(Collection<Long> scopeIds, String metricType, long offset, int limit) {
        return metricMapper.selectPage(scopeIds, metricType, offset, limit)
                .stream().map(MetricConverter::toEntity).toList();
    }

    @Override
    public long countByScope(Collection<Long> scopeIds, String metricType) {
        return metricMapper.countByScope(scopeIds, metricType);
    }

    @Override
    public List<DailyMetric> selectTrend(Long familyMemberId, String metricType) {
        return metricMapper.selectTrend(familyMemberId, metricType)
                .stream().map(MetricConverter::toEntity).toList();
    }
}
