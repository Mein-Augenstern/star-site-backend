package com.xp.starsite.health.service;

import com.xp.starsite.health.dto.IndicatorBatchDTO;
import com.xp.starsite.health.dto.ReportCreateDTO;
import com.xp.starsite.health.vo.HealthReportVO;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.TrendPointVO;

import java.util.List;

/**
 * 体检报告服务
 */
public interface HealthService {

    /** 上传报告元数据 */
    HealthReportVO createReport(ReportCreateDTO dto);

    /** 报告分页列表 */
    PageVO<HealthReportVO> listReports(Long familyMemberId, Integer year, long page, long size);

    /** 报告详情（含指标） */
    HealthReportVO getDetail(Long id);

    /** 批量录入指标（异常标志按参考区间自动判） */
    void addIndicators(Long reportId, IndicatorBatchDTO dto);

    /** 删除报告（逻辑删） */
    void deleteReport(Long id);

    /** 某成员某指标历年趋势 */
    List<TrendPointVO> trends(Long familyMemberId, String itemName);

    /** 关键词搜索（标题/医院/总结/指标名） */
    List<HealthReportVO> search(String keyword, Long familyMemberId);
}
