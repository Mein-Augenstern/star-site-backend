package com.xp.starsite.health.service.impl;

import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.api.dto.health.HealthIndicatorDTO;
import com.xp.starsite.api.dto.health.HealthReportDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.health.dto.IndicatorBatchDTO;
import com.xp.starsite.health.dto.IndicatorDTO;
import com.xp.starsite.health.dto.ReportCreateDTO;
import com.xp.starsite.health.entity.HealthIndicator;
import com.xp.starsite.health.entity.HealthReport;
import com.xp.starsite.health.repository.HealthIndicatorRepository;
import com.xp.starsite.health.repository.HealthReportRepository;
import com.xp.starsite.health.service.ReportService;
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

/**
 * 体检报告服务实现。
 * <p>
 * 越权防线：凡带 familyMemberId 的入参先经 {@link FamilyOwnershipGuard} 校验归属；
 * 列表/搜索不传 familyMemberId 时强制收口在「自己家庭的成员集合」内；
 * fail-closed：family 服务不可用即拒绝请求。
 * 数据访问经仓储层（{@link HealthReportRepository}/{@link HealthIndicatorRepository}，
 * 实现位于 core-service、底层调用 dal DAO），业务层只见领域模型、零 DAO 依赖。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final HealthReportRepository reportRepository;
    private final HealthIndicatorRepository indicatorRepository;
    private final FamilyOwnershipGuard ownershipGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthReportVO createReport(Long memberId, ReportCreateDTO dto) {
        ownershipGuard.requireOwned(memberId, dto.getFamilyMemberId());
        HealthReport report = new HealthReport();
        report.setFamilyMemberId(dto.getFamilyMemberId());
        report.setReportYear(dto.getReportYear());
        report.setCheckupDate(dto.getCheckupDate());
        report.setHospital(dto.getHospital());
        // 标题缺省："<年份>年体检报告"
        report.setTitle(StringUtils.hasText(dto.getTitle())
                ? dto.getTitle() : dto.getReportYear() + "年体检报告");
        report.setSummary(dto.getSummary());
        report.setFileId(dto.getFileId());
        report.setRemark(dto.getRemark());
        reportRepository.save(report);
        log.info("上传体检报告: id={}, member={}, year={}", report.getId(), dto.getFamilyMemberId(), dto.getReportYear());
        return toVO(report, null);
    }

    @Override
    public HealthReportDTO getInternalDetail(Long memberId, Long reportId) {
        return toInternalDto(getDetail(memberId, reportId));
    }

    @Override
    public List<HealthReportDTO> listInternal(Long memberId, Long familyMemberId) {
        return listReports(memberId, familyMemberId, null, 1, 20).getRecords()
                .stream().map(vo -> {
                    HealthReportDTO dto = toInternalDto(vo);
                    dto.setIndicators(null);
                    return dto;
                }).toList();
    }

    /** VO → 内部契约 DTO（映射收敛在 common-biz，供 facade 出参；控制器零映射） */
    private HealthReportDTO toInternalDto(HealthReportVO vo) {
        HealthReportDTO dto = new HealthReportDTO();
        dto.setId(vo.getId());
        dto.setFamilyMemberId(vo.getFamilyMemberId());
        dto.setReportYear(vo.getReportYear());
        dto.setCheckupDate(vo.getCheckupDate());
        dto.setHospital(vo.getHospital());
        dto.setTitle(vo.getTitle());
        dto.setSummary(vo.getSummary());
        dto.setFileId(vo.getFileId());
        dto.setRemark(vo.getRemark());
        dto.setCreatedAt(vo.getCreatedAt());
        if (vo.getIndicators() != null) {
            dto.setIndicators(vo.getIndicators().stream().map(i -> {
                HealthIndicatorDTO ind = new HealthIndicatorDTO();
                ind.setId(i.getId());
                ind.setReportId(i.getReportId());
                ind.setReportYear(i.getReportYear());
                ind.setItemName(i.getItemName());
                ind.setItemGroup(i.getItemGroup());
                ind.setValueNum(i.getValueNum());
                ind.setValueText(i.getValueText());
                ind.setUnit(i.getUnit());
                ind.setRefLow(i.getRefLow());
                ind.setRefHigh(i.getRefHigh());
                ind.setAbnormalFlag(i.getAbnormalFlag());
                ind.setSortNo(i.getSortNo());
                ind.setCreatedAt(i.getCreatedAt());
                return ind;
            }).toList());
        }
        return dto;
    }

    @Override
    public PageVO<HealthReportVO> listReports(Long memberId, Long familyMemberId, Integer year, long page, long size) {
        Set<Long> scope = ownedScope(memberId, familyMemberId);
        if (scope.isEmpty()) {
            return PageVO.of(List.of(), 0, page, size);
        }
        long total = reportRepository.countByScope(scope, year);
        if (total == 0) {
            return PageVO.of(List.of(), 0, page, size);
        }
        long offset = (page - 1) * size;
        List<HealthReportVO> vos = reportRepository.pageByScope(scope, year, offset, (int) size)
                .stream().map(r -> toVO(r, null)).toList();
        return PageVO.of(vos, total, page, size);
    }

    @Override
    public HealthReportVO getDetail(Long memberId, Long id) {
        HealthReport report = requireOwnedReport(memberId, id);
        List<HealthIndicatorVO> indicators = indicatorRepository.findByReportId(id)
                .stream().map(this::toIndicatorVO).toList();
        return toVO(report, indicators);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addIndicators(Long memberId, Long reportId, IndicatorBatchDTO dto) {
        HealthReport report = requireOwnedReport(memberId, reportId);
        // 重录覆盖：先清掉该报告旧指标
        indicatorRepository.deleteByReportId(reportId);
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
            indicatorRepository.save(indicator);
            sort++;
        }
        log.info("录入指标: reportId={}, count={}", reportId, dto.getIndicators().size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long memberId, Long id) {
        requireOwnedReport(memberId, id);
        reportRepository.markDeleted(id);
    }

    @Override
    public List<HealthReportVO> search(Long memberId, String keyword, Long familyMemberId) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        Set<Long> scope = ownedScope(memberId, familyMemberId);
        if (scope.isEmpty()) {
            return List.of();
        }
        String kw = keyword.trim();
        // 命中指标名的报告 id（指标名命中也算报告命中）
        List<Long> reportIds = indicatorRepository.findReportIdsByKeywordLike(kw);
        return reportRepository.search(scope, kw, reportIds).stream().map(r -> toVO(r, null)).toList();
    }

    @Override
    public List<TrendPointVO> trends(Long memberId, Long familyMemberId, String itemName) {
        ownershipGuard.requireOwned(memberId, familyMemberId);
        return indicatorRepository.trendByItem(familyMemberId, itemName)
                .stream().map(this::toTrendVO).toList();
    }

    /** 按 id 取报告并校验归属：不存在报 400，非本人家庭成员报 403 */
    private HealthReport requireOwnedReport(Long memberId, Long id) {
        HealthReport report = reportRepository.findById(id).orElse(null);
        if (report == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "报告不存在");
        }
        ownershipGuard.requireOwned(memberId, report.getFamilyMemberId());
        return report;
    }

    /**
     * 列表/搜索的数据范围：指定 familyMemberId 时校验其归属后单查；
     * 未指定时限定在自己家庭全部成员内（空家庭 = 空结果）。
     */
    private Set<Long> ownedScope(Long memberId, Long familyMemberId) {
        if (familyMemberId != null) {
            ownershipGuard.requireOwned(memberId, familyMemberId);
            return Set.of(familyMemberId);
        }
        return Set.copyOf(ownershipGuard.listOwnedMemberIds(memberId));
    }

    /** 异常标志判定：0正常 1偏高 2偏低（有数值和参考区间才判） */
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

    private TrendPointVO toTrendVO(HealthIndicator i) {
        TrendPointVO vo = new TrendPointVO();
        vo.setReportYear(i.getReportYear());
        vo.setReportId(i.getReportId());
        vo.setValueNum(i.getValueNum());
        vo.setValueText(i.getValueText());
        vo.setUnit(i.getUnit());
        vo.setRefLow(i.getRefLow());
        vo.setRefHigh(i.getRefHigh());
        vo.setAbnormalFlag(i.getAbnormalFlag());
        return vo;
    }
}
