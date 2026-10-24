package com.xp.starsite.health.repository.impl;

import com.xp.starsite.health.converter.ReportConverter;
import com.xp.starsite.health.dal.dataobject.HealthReportDO;
import com.xp.starsite.health.entity.HealthReport;
import com.xp.starsite.health.mapper.HealthReportMapper;
import com.xp.starsite.health.repository.HealthReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 体检报告仓储实现：调用 dal 的 {@link HealthReportMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class HealthReportRepositoryImpl implements HealthReportRepository {

    private final HealthReportMapper reportMapper;

    @Override
    public HealthReport save(HealthReport report) {
        HealthReportDO row = ReportConverter.toDO(report);
        reportMapper.insert(row);
        report.setId(row.getId());
        return report;
    }

    @Override
    public Optional<HealthReport> findById(Long id) {
        return Optional.ofNullable(reportMapper.selectById(id)).map(ReportConverter::toEntity);
    }

    @Override
    public List<HealthReport> pageByScope(Collection<Long> scopeIds, Integer year, long offset, int limit) {
        return reportMapper.selectPage(scopeIds, year, offset, limit)
                .stream().map(ReportConverter::toEntity).toList();
    }

    @Override
    public long countByScope(Collection<Long> scopeIds, Integer year) {
        return reportMapper.countByScope(scopeIds, year);
    }

    @Override
    public List<HealthReport> search(Collection<Long> scopeIds, String keyword, Collection<Long> reportIds) {
        return reportMapper.search(scopeIds, keyword, reportIds)
                .stream().map(ReportConverter::toEntity).toList();
    }

    @Override
    public void markDeleted(Long id) {
        reportMapper.markDeleted(id);
    }

    @Override
    public void updateInterpretation(Long id, String interpretation, LocalDateTime interpretedAt) {
        reportMapper.updateInterpretation(id, interpretation, interpretedAt);
    }
}
