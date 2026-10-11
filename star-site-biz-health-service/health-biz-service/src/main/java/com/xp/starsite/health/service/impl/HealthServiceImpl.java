package com.xp.starsite.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.health.dto.IndicatorBatchDTO;
import com.xp.starsite.health.dto.IndicatorDTO;
import com.xp.starsite.health.dto.MetricBatchDTO;
import com.xp.starsite.health.dto.PregnancyUpsertDTO;
import com.xp.starsite.health.dto.ReportCreateDTO;
import com.xp.starsite.health.dto.VisitCreateDTO;
import com.xp.starsite.health.entity.DailyMetric;
import com.xp.starsite.health.entity.HealthIndicator;
import com.xp.starsite.health.entity.HealthReport;
import com.xp.starsite.health.entity.PregnancyProfile;
import com.xp.starsite.health.entity.VisitRecord;
import com.xp.starsite.health.mapper.DailyMetricMapper;
import com.xp.starsite.health.mapper.HealthIndicatorMapper;
import com.xp.starsite.health.mapper.HealthReportMapper;
import com.xp.starsite.health.mapper.PregnancyProfileMapper;
import com.xp.starsite.health.mapper.VisitRecordMapper;
import com.xp.starsite.health.service.HealthService;
import com.xp.starsite.health.vo.HealthIndicatorVO;
import com.xp.starsite.health.vo.HealthReportVO;
import com.xp.starsite.health.vo.MetricVO;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.PregnancyVO;
import com.xp.starsite.health.vo.TrendPointVO;
import com.xp.starsite.health.vo.VisitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 体检报告服务实现。
 * <p>
 * 越权防线：凡带 familyMemberId 的入参先经 {@link FamilyOwnershipGuard} 校验归属；
 * 列表/搜索不传 familyMemberId 时也强制收口在「自己家庭的成员集合」内，
 * 不能看到其他账号的数据。fail-closed：family 服务不可用即拒绝请求。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final HealthReportMapper reportMapper;
    private final HealthIndicatorMapper indicatorMapper;
    private final VisitRecordMapper visitMapper;
    private final DailyMetricMapper metricMapper;
    private final PregnancyProfileMapper pregnancyMapper;
    private final FamilyOwnershipGuard ownershipGuard;

    @Override
    public HealthReportVO createReport(Long memberId, ReportCreateDTO dto) {
        ownershipGuard.requireOwned(memberId, dto.getFamilyMemberId());
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
    public PageVO<HealthReportVO> listReports(Long memberId, Long familyMemberId, Integer year, long page, long size) {
        Set<Long> scope = ownedScope(memberId, familyMemberId);
        if (scope.isEmpty()) {
            return PageVO.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<HealthReport> wrapper = new LambdaQueryWrapper<HealthReport>()
                .in(HealthReport::getFamilyMemberId, scope)
                .eq(year != null, HealthReport::getReportYear, year)
                .orderByDesc(HealthReport::getCheckupDate)
                .orderByDesc(HealthReport::getId);
        Page<HealthReport> result = reportMapper.selectPage(Page.of(page, size), wrapper);
        List<HealthReportVO> vos = result.getRecords().stream()
                .map(r -> toVO(r, null)).toList();
        return PageVO.of(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public HealthReportVO getDetail(Long memberId, Long id) {
        HealthReport report = requireOwnedReport(memberId, id);
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
    public void addIndicators(Long memberId, Long reportId, IndicatorBatchDTO dto) {
        HealthReport report = requireOwnedReport(memberId, reportId);
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
    public void deleteReport(Long memberId, Long id) {
        requireOwnedReport(memberId, id);
        reportMapper.deleteById(id);
    }

    @Override
    public List<TrendPointVO> trends(Long memberId, Long familyMemberId, String itemName) {
        ownershipGuard.requireOwned(memberId, familyMemberId);
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
                    vo.setRefLow(i.getRefLow());
                    vo.setRefHigh(i.getRefHigh());
                    vo.setAbnormalFlag(i.getAbnormalFlag());
                    return vo;
                }).toList();
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
        // 命中指标名的报告 id
        Set<Long> reportIds = indicatorMapper.selectList(
                        new LambdaQueryWrapper<HealthIndicator>()
                                .like(HealthIndicator::getItemName, kw))
                .stream().map(HealthIndicator::getReportId).collect(Collectors.toSet());

        LambdaQueryWrapper<HealthReport> wrapper = new LambdaQueryWrapper<HealthReport>()
                .in(HealthReport::getFamilyMemberId, scope)
                .and(w -> w.like(HealthReport::getTitle, kw)
                        .or().like(HealthReport::getHospital, kw)
                        .or().like(HealthReport::getSummary, kw)
                        .or().in(!reportIds.isEmpty(), HealthReport::getId, reportIds))
                .orderByDesc(HealthReport::getCheckupDate)
                .last("LIMIT 50");
        return reportMapper.selectList(wrapper).stream().map(r -> toVO(r, null)).toList();
    }

    @Override
    public VisitVO createVisit(Long memberId, VisitCreateDTO dto) {
        ownershipGuard.requireOwned(memberId, dto.getFamilyMemberId());
        VisitRecord visit = new VisitRecord();
        visit.setFamilyMemberId(dto.getFamilyMemberId());
        visit.setVisitDate(dto.getVisitDate());
        visit.setHospital(dto.getHospital());
        visit.setDepartment(dto.getDepartment());
        visit.setDiagnosis(dto.getDiagnosis());
        visit.setMedication(dto.getMedication());
        visit.setCost(dto.getCost());
        visit.setRemark(dto.getRemark());
        visitMapper.insert(visit);
        log.info("新建就医记录: id={}, member={}, date={}", visit.getId(), dto.getFamilyMemberId(), dto.getVisitDate());
        return toVisitVO(visit);
    }

    @Override
    public PageVO<VisitVO> listVisits(Long memberId, Long familyMemberId, long page, long size) {
        Set<Long> scope = ownedScope(memberId, familyMemberId);
        if (scope.isEmpty()) {
            return PageVO.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<VisitRecord> wrapper = new LambdaQueryWrapper<VisitRecord>()
                .in(VisitRecord::getFamilyMemberId, scope)
                .orderByDesc(VisitRecord::getVisitDate)
                .orderByDesc(VisitRecord::getId);
        Page<VisitRecord> result = visitMapper.selectPage(Page.of(page, size), wrapper);
        List<VisitVO> vos = result.getRecords().stream().map(this::toVisitVO).toList();
        return PageVO.of(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void deleteVisit(Long memberId, Long id) {
        VisitRecord visit = visitMapper.selectById(id);
        if (visit == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "就医记录不存在");
        }
        ownershipGuard.requireOwned(memberId, visit.getFamilyMemberId());
        visitMapper.deleteById(id);
    }

    @Override
    public List<MetricVO> addMetrics(Long memberId, MetricBatchDTO dto) {
        List<MetricVO> result = new java.util.ArrayList<>();
        for (MetricBatchDTO.Item item : dto.getItems()) {
            ownershipGuard.requireOwned(memberId, item.getFamilyMemberId());
        }
        for (MetricBatchDTO.Item item : dto.getItems()) {
            DailyMetric m = new DailyMetric();
            m.setFamilyMemberId(item.getFamilyMemberId());
            m.setMetricDate(item.getMetricDate());
            m.setMetricType(item.getMetricType());
            m.setValueNum(item.getValueNum());
            m.setRemark(item.getRemark());
            metricMapper.insert(m);
            result.add(toMetricVO(m));
        }
        log.info("录入日常指标: {} 条", dto.getItems().size());
        return result;
    }

    @Override
    public PageVO<MetricVO> listMetrics(Long memberId, Long familyMemberId, String metricType, long page, long size) {
        Set<Long> scope = ownedScope(memberId, familyMemberId);
        if (scope.isEmpty()) {
            return PageVO.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<DailyMetric> wrapper = new LambdaQueryWrapper<DailyMetric>()
                .in(DailyMetric::getFamilyMemberId, scope)
                .eq(StringUtils.hasText(metricType), DailyMetric::getMetricType, metricType)
                .orderByDesc(DailyMetric::getMetricDate)
                .orderByDesc(DailyMetric::getId);
        Page<DailyMetric> result = metricMapper.selectPage(Page.of(page, size), wrapper);
        List<MetricVO> vos = result.getRecords().stream().map(this::toMetricVO).toList();
        return PageVO.of(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void deleteMetric(Long memberId, Long id) {
        DailyMetric m = metricMapper.selectById(id);
        if (m == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "指标记录不存在");
        }
        ownershipGuard.requireOwned(memberId, m.getFamilyMemberId());
        metricMapper.deleteById(id);
    }

    @Override
    public List<com.xp.starsite.health.vo.MetricTrendPoint> metricTrends(Long memberId, Long familyMemberId, String metricType) {
        ownershipGuard.requireOwned(memberId, familyMemberId);
        return metricMapper.selectList(
                        new LambdaQueryWrapper<DailyMetric>()
                                .eq(DailyMetric::getFamilyMemberId, familyMemberId)
                                .eq(DailyMetric::getMetricType, metricType)
                                .orderByAsc(DailyMetric::getMetricDate))
                .stream()
                .map(m -> new com.xp.starsite.health.vo.MetricTrendPoint(m.getMetricDate(), m.getValueNum()))
                .toList();
    }

    /** 标准产检/关键节点（孕周窗口）：NT、唐筛、大排畸、糖耐是四大关键筛查 */
    private static final Object[][] MILESTONES = {
            {6, 8, "首次检查·建档B超", "确认宫内孕、胎心"},
            {11, 13, "NT 检查（早期唐筛）", "孕 11-13+6 窗口，勿错过"},
            {15, 20, "唐筛 / 无创 DNA", "中期血清学筛查"},
            {20, 24, "大排畸（系统超声）", "胎儿结构筛查"},
            {24, 28, "糖耐量试验 OGTT", "妊娠糖尿病筛查，需空腹"},
            {28, 32, "小排畸·胎心", "复查生长发育"},
            {32, 36, "胎心监护起步", "每 2 周产检"},
            {37, 40, "足月·每周产检", "随时准备待产"},
    };

    @Override
    public PregnancyVO upsertPregnancy(Long memberId, PregnancyUpsertDTO dto) {
        ownershipGuard.requireOwned(memberId, dto.getFamilyMemberId());
        PregnancyProfile profile = pregnancyMapper.selectOne(
                new LambdaQueryWrapper<PregnancyProfile>()
                        .eq(PregnancyProfile::getFamilyMemberId, dto.getFamilyMemberId()));
        if (profile == null) {
            profile = new PregnancyProfile();
            profile.setFamilyMemberId(dto.getFamilyMemberId());
            profile.setEnded(0);
        }
        profile.setLmpDate(dto.getLmpDate());
        profile.setHeightCm(dto.getHeightCm());
        profile.setPrePregnancyWeight(dto.getPrePregnancyWeight());
        if (profile.getId() == null) {
            pregnancyMapper.insert(profile);
        } else {
            pregnancyMapper.updateById(profile);
        }
        return toPregnancyVO(profile);
    }

    @Override
    public PregnancyVO getPregnancy(Long memberId, Long familyMemberId) {
        ownershipGuard.requireOwned(memberId, familyMemberId);
        PregnancyProfile profile = pregnancyMapper.selectOne(
                new LambdaQueryWrapper<PregnancyProfile>()
                        .eq(PregnancyProfile::getFamilyMemberId, familyMemberId));
        return profile == null ? null : toPregnancyVO(profile);
    }

    /** 孕周中枢：以 LMP 为锚点计算孕周/预产期/阶段/产检计划/增重建议 */
    private PregnancyVO toPregnancyVO(PregnancyProfile p) {
        PregnancyVO vo = new PregnancyVO();
        vo.setFamilyMemberId(p.getFamilyMemberId());
        vo.setLmpDate(p.getLmpDate());
        LocalDate today = LocalDate.now();
        long days = java.time.temporal.ChronoUnit.DAYS.between(p.getLmpDate(), today);
        if (days < 0) {
            days = 0;
        }
        int week = (int) (days / 7);
        vo.setGestationalWeek(week);
        vo.setGestationalDay((int) (days % 7));
        vo.setDueDate(p.getLmpDate().plusDays(280));
        vo.setDaysToDue(java.time.temporal.ChronoUnit.DAYS.between(today, vo.getDueDate()));
        vo.setStage(week <= 12 ? "孕早期" : week <= 27 ? "孕中期" : "孕晚期");

        if (p.getHeightCm() != null && p.getPrePregnancyWeight() != null
                && p.getHeightCm().signum() > 0) {
            double h = p.getHeightCm().doubleValue() / 100;
            double bmi = p.getPrePregnancyWeight().doubleValue() / (h * h);
            vo.setPreBmi(java.math.BigDecimal.valueOf(Math.round(bmi * 10) / 10.0));
            // IOM 单胎全孕期增重标准
            String advice = bmi < 18.5 ? "12.5~18.0" : bmi < 25 ? "11.5~16.0" : bmi < 30 ? "7.0~11.5" : "5.0~9.0";
            vo.setWeightGainAdvice(advice);
        }

        List<PregnancyVO.Milestone> ms = new java.util.ArrayList<>();
        for (Object[] m : MILESTONES) {
            PregnancyVO.Milestone item = new PregnancyVO.Milestone();
            int from = (int) m[0];
            int to = (int) m[1];
            item.setFromWeek(from);
            item.setToWeek(to);
            item.setName((String) m[2]);
            item.setTip((String) m[3]);
            item.setStatus(week < from ? "pending" : week <= to ? "current" : "done");
            ms.add(item);
        }
        vo.setMilestones(ms);
        return vo;
    }

    /** 按 id 取报告并校验归属：不存在报 400，非本人家庭成员报 403 */
    private HealthReport requireOwnedReport(Long memberId, Long id) {
        HealthReport report = reportMapper.selectById(id);
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

    private VisitVO toVisitVO(VisitRecord v) {
        VisitVO vo = new VisitVO();
        BeanUtils.copyProperties(v, vo);
        return vo;
    }

    private MetricVO toMetricVO(DailyMetric m) {
        MetricVO vo = new MetricVO();
        BeanUtils.copyProperties(m, vo);
        return vo;
    }
}
