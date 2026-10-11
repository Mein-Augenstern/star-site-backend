package com.xp.starsite.health.service;

import com.xp.starsite.health.dto.IndicatorBatchDTO;
import com.xp.starsite.health.dto.MetricBatchDTO;
import com.xp.starsite.health.dto.PregnancyUpsertDTO;
import com.xp.starsite.health.dto.ReportCreateDTO;
import com.xp.starsite.health.dto.VisitCreateDTO;
import com.xp.starsite.health.vo.HealthReportVO;
import com.xp.starsite.health.vo.MetricVO;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.PregnancyVO;
import com.xp.starsite.health.vo.TrendPointVO;
import com.xp.starsite.health.vo.VisitVO;

import java.util.List;

/**
 * 体检报告服务。
 * 所有方法首参 memberId 为当前登录顶梁柱（Controller 从 JWT attribute 取），
 * 用于 family 归属校验/范围收口，防止横向越权。
 */
public interface HealthService {

    /** 上传报告元数据（校验 familyMemberId 归属） */
    HealthReportVO createReport(Long memberId, ReportCreateDTO dto);

    /** 报告分页列表（强制限定在自己家庭成员范围内） */
    PageVO<HealthReportVO> listReports(Long memberId, Long familyMemberId, Integer year, long page, long size);

    /** 报告详情（含指标；他人家庭报告返回 403） */
    HealthReportVO getDetail(Long memberId, Long id);

    /** 批量录入指标（异常标志按参考区间自动判） */
    void addIndicators(Long memberId, Long reportId, IndicatorBatchDTO dto);

    /** 删除报告（逻辑删） */
    void deleteReport(Long memberId, Long id);

    /** 某成员某指标历年趋势 */
    List<TrendPointVO> trends(Long memberId, Long familyMemberId, String itemName);

    /** 关键词搜索（标题/医院/总结/指标名；限定自己家庭成员范围） */
    List<HealthReportVO> search(Long memberId, String keyword, Long familyMemberId);

    /** 新建就医记录（校验 familyMemberId 归属） */
    VisitVO createVisit(Long memberId, VisitCreateDTO dto);

    /** 就医记录分页列表（不传成员=全家，归属收口） */
    PageVO<VisitVO> listVisits(Long memberId, Long familyMemberId, long page, long size);

    /** 删除就医记录（逻辑删，先校验归属） */
    void deleteVisit(Long memberId, Long id);

    /** 批量录入日常指标（血压产生高压+低压两条；校验归属） */
    List<MetricVO> addMetrics(Long memberId, MetricBatchDTO dto);

    /** 日常指标分页列表（不传成员=全家，归属收口） */
    PageVO<MetricVO> listMetrics(Long memberId, Long familyMemberId, String metricType, long page, long size);

    /** 删除日常指标（逻辑删，先校验归属） */
    void deleteMetric(Long memberId, Long id);

    /** 设置/更新孕期档案（重复提交覆盖），返回含孕周/产检计划的计算视图 */
    PregnancyVO upsertPregnancy(Long memberId, PregnancyUpsertDTO dto);

    /** 孕期档案查询（无档案返回 null data） */
    PregnancyVO getPregnancy(Long memberId, Long familyMemberId);

    /** 日常指标趋势（按测量日期升序，孕期体重曲线等） */
    List<com.xp.starsite.health.vo.MetricTrendPoint> metricTrends(Long memberId, Long familyMemberId, String metricType);
}
