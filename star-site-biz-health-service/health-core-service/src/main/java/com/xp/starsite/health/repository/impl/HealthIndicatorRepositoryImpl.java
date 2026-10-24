package com.xp.starsite.health.repository.impl;

import com.xp.starsite.health.converter.IndicatorConverter;
import com.xp.starsite.health.dal.dataobject.HealthIndicatorDO;
import com.xp.starsite.health.entity.HealthIndicator;
import com.xp.starsite.health.mapper.HealthIndicatorMapper;
import com.xp.starsite.health.repository.HealthIndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 体检指标仓储实现：调用 dal 的 {@link HealthIndicatorMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class HealthIndicatorRepositoryImpl implements HealthIndicatorRepository {

    private final HealthIndicatorMapper indicatorMapper;

    @Override
    public HealthIndicator save(HealthIndicator indicator) {
        HealthIndicatorDO row = IndicatorConverter.toDO(indicator);
        indicatorMapper.insert(row);
        indicator.setId(row.getId());
        return indicator;
    }

    @Override
    public List<HealthIndicator> findByReportId(Long reportId) {
        return indicatorMapper.selectByReportId(reportId)
                .stream().map(IndicatorConverter::toEntity).toList();
    }

    @Override
    public void deleteByReportId(Long reportId) {
        indicatorMapper.deleteByReportId(reportId);
    }

    @Override
    public List<Long> findReportIdsByKeywordLike(String keyword) {
        return indicatorMapper.selectByKeywordLike(keyword)
                .stream().map(HealthIndicatorDO::getReportId).toList();
    }

    @Override
    public List<HealthIndicator> trendByItem(Long familyMemberId, String itemName) {
        return indicatorMapper.selectTrend(familyMemberId, itemName)
                .stream().map(IndicatorConverter::toEntity).toList();
    }
}
