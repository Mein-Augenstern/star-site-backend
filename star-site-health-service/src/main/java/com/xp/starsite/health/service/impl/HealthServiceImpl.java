package com.xp.starsite.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.health.dto.IndicatorBatchDTO;
import com.xp.starsite.health.dto.IndicatorDTO;
import com.xp.starsite.health.dto.ReportCreateDTO;
import com.xp.starsite.health.entity.HealthIndicator;
import com.xp.starsite.health.entity.HealthReport;
import com.xp.starsite.health.mapper.HealthIndicatorMapper;
import com.xp.starsite.health.mapper.HealthReportMapper;
import com.xp.starsite.health.service.HealthService;
import com.xp.starsite.health.vo.HealthIndicatorVO;
import com.xp.starsite.health.vo.HealthReportVO;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 体检报告服务实现
 * TODO：familyMemberId 归属校验（后续用 OpenFeign 调 family 服务核对，当前自用场景从简）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final HealthReportMapper reportMapper;
    private final HealthIndicatorMapper indicatorMapper;

    @Override
    public HealthReportVO createReport(ReportCreateDTO dto) {
        HealthReport report = new HealthReport();
        report.setFamilyMemberId(dto.getFamilyMemberId());
        report.setReportYear(dto.getReportYear());
        report.setCheckupDate(dto.getCheckupDate());
        report.setHospital(dto.getHospital());
        report.setTitle(StringUtils.hasText(dto.getTitle())
                ? dto.getTitle() : dto.getReportYear() + "年体检报告");
        report.setSummary(dto.getSummary());
        report.setFileId(dto.getFileId());
        report.setRemark(dto.getRemark());
        reportMapper.insert(report);
        log.info("上传体检报告: id={}, member={}, year={}", report.getId(), dto.getFamilyMemberId(), dto.getReportYear());
        return toVO(report, null);
    }

    @Override
    public PageVO<HealthReportVO> listReports(Long familyMemberId, Integer year, long page, long size) {
        LambdaQueryWrapper<HealthReport> wrapper = new LambdaQueryWrapper<HealthReport>()
                .eq(familyMemberId != null, HealthReport::getFamilyMemberId, familyMemberId)
                .eq(year != null, HealthReport::getReportYear, year)
                .orderByDesc(HealthReport::getCheckupDate)
                .orderByDesc(HealthReport::getId);
        Page<HealthReport> result = reportMapper.selectPage(Page.of(page, size), wrapper);
        List<HealthReportVO> vos = result.getRecords().stream()
                .map(r -> toVO(r, null)).toList();
        return PageVO.of(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public HealthReportVO getDetail(Long id) {
        HealthReport report = requireReport(id);
        List<HealthIndicatorVO> indicators = indicatorMapper.selectList(
                        new LambdaQueryWrapper<HealthIndicator>()
                                .eq(HealthIndicator::getReportId, id)
                                .orderByAsc(HealthIndicator::getSortNo)
                                .orderByAsc(HealthIndicator::getId))
                .stream().map(this::toIndicatorVO).toList();
        return toVO(report, indicators);
    }

    @Override
    @Transactional
    public void addIndicators(Long reportId, IndicatorBatchDTO dto) {
        HealthReport report = requireReport(reportId);
        // 重录覆盖：先清掉该报告旧指标
        indicatorMapper.delete(new LambdaQueryWrapper<HealthIndicator>()
                .eq(HealthIndicator::getReportId, reportId));
        int sort = 0;
        for (IndicatorDTO i : dto.getIndicators()) {
            HealthIndicator indicator = new HealthIndicator();
            indicator.setReportId(reportId);
            indicator.setFamilyMemberId(report.getFamilyMemberId());
            indicator.setReportYear(report.getReportYear());
            indicator.setItemName(i.getItemName());
            indicator.setItemGroup(i.getItemGroup());
            indicator.setValueNum(i.getValueNum());
            indicator.setValueText(i.getValueText());
            indicator.setUnit(i.getUnit());
            indicator.setRefLow(i.getRefLow());
            indicator.setRefHigh(i.getRefHigh());
            indicator.setAbnormalFlag(judgeAbnormal(i));
            indicator.setSortNo(i.getSortNo() != null ? i.getSortNo() : sort);
            indicatorMapper.insert(indicator);
            sort++;
        }
        log.info("录入指标: reportId={}, count={}", reportId, dto.getIndicators().size());
    }

    @Override
    public void deleteReport(Long id) {
        requireReport(id);
        reportMapper.deleteById(id);
    }

    @Override
    public List<TrendPointVO> trends(Long familyMemberId, String itemName) {
        return indicatorMapper.selectList(
                        new LambdaQueryWrapper<HealthIndicator>()
                                .eq(HealthIndicator::getFamilyMemberId, familyMemberId)
                                .eq(HealthIndicator::getItemName, itemName)
                                .orderByAsc(HealthIndicator::getReportYear))
                .stream().map(i -> {
                    TrendPointVO vo = new TrendPointVO();
                    vo.setReportYear(i.getReportYear());
                    vo.setReportId(i.getReportId());
                    vo.setValueNum(i.getValueNum());
                    vo.setValueText(i.getValueText());
                    vo.setUnit(i.getUnit());
                    vo.setAbnormalFlag(i.getAbnormalFlag());
                    return vo;
                }).toList();
    }

    @Override
    public List<HealthReportVO> search(String keyword, Long familyMemberId) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        String kw = keyword.trim();
        // 命中指标名的报告 id
        Set<Long> reportIds = indicatorMapper.selectList(
                        new LambdaQueryWrapper<HealthIndicator>()
                                .like(HealthIndicator::getItemName, kw))
                .stream().map(HealthIndicator::getReportId).collect(Collectors.toSet());

        LambdaQueryWrapper<HealthReport> wrapper = new LambdaQueryWrapper<HealthReport>()
                .eq(familyMemberId != null, HealthReport::getFamilyMemberId, familyMemberId)
                .and(w -> w.like(HealthReport::getTitle, kw)
                        .or().like(HealthReport::getHospital, kw)
                        .or().like(HealthReport::getSummary, kw)
                        .or().in(!reportIds.isEmpty(), HealthReport::getId, reportIds))
                .orderByDesc(HealthReport::getCheckupDate)
                .last("LIMIT 50");
        return reportMapper.selectList(wrapper).stream().map(r -> toVO(r, null)).toList();
    }

    private HealthReport requireReport(Long id) {
        HealthReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "报告不存在");
        }
        return report;
    }

    /** 0正常 1偏高 2偏低（有数值和参考区间才判） */
    private int judgeAbnormal(IndicatorDTO i) {
        if (i.getValueNum() == null) {
            return 0;
        }
        if (i.getRefHigh() != null && i.getValueNum().compareTo(i.getRefHigh()) > 0) {
            return 1;
        }
        if (i.getRefLow() != null && i.getValueNum().compareTo(i.getRefLow()) < 0) {
            return 2;
        }
        return 0;
    }

    private HealthReportVO toVO(HealthReport report, List<HealthIndicatorVO> indicators) {
        HealthReportVO vo = new HealthReportVO();
        BeanUtils.copyProperties(report, vo);
        vo.setIndicators(indicators);
        return vo;
    }

    private HealthIndicatorVO toIndicatorVO(HealthIndicator i) {
        HealthIndicatorVO vo = new HealthIndicatorVO();
        BeanUtils.copyProperties(i, vo);
        return vo;
    }
}
