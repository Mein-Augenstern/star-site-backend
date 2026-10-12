package com.xp.starsite.health.service;

import com.xp.starsite.health.dto.IndicatorBatchDTO;
import com.xp.starsite.health.dto.ReportCreateDTO;
import com.xp.starsite.health.vo.HealthReportVO;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.TrendPointVO;

import java.util.List;

/**
 * 体检报告服务：报告建档/查询/删除、报告指标批量录入、报告搜索与指标趋势。
 * <p>
 * 归属防线约定：所有方法首参 memberId 为当前登录顶梁柱（Controller 从 JWT attribute 取），
 * 带 familyMemberId 的写入先经 {@link com.xp.starsite.api.client.FamilyOwnershipGuard} 校验；
 * 列表/搜索不传 familyMemberId 时强制收口在「自己家庭的成员集合」内，fail-closed。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface ReportService {

    /**
     * 上传报告元数据（新建建档）
     *
     * @param memberId 当前登录账号
     * @param dto      报告元数据（家庭成员/年份/医院/标题等）
     * @return 新建报告视图（不含指标）
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    HealthReportVO createReport(Long memberId, ReportCreateDTO dto);

    /**
     * 报告分页列表（按体检日期倒序）
     *
     * @param familyMemberId 可选，指定成员则单查（校验归属）；不传=全家范围
     * @param year           可选，按报告年份过滤
     */
    PageVO<HealthReportVO> listReports(Long memberId, Long familyMemberId, Integer year, long page, long size);

    /**
     * 报告详情（含全部指标，按 sortNo 排序）
     *
     * @throws com.xp.starsite.common.exception.BizException 400 报告不存在；403 非本人家庭成员的报告
     */
    HealthReportVO getDetail(Long memberId, Long id);

    /**
     * 批量录入报告指标（重录覆盖语义：先清空该报告旧指标再全量插入；
     * 异常标志按参考区间自动判定，无需前端传入）
     */
    void addIndicators(Long memberId, Long reportId, IndicatorBatchDTO dto);

    /**
     * 删除报告（逻辑删）
     *
     * @throws com.xp.starsite.common.exception.BizException 400 报告不存在；403 非本人家庭成员的报告
     */
    void deleteReport(Long memberId, Long id);

    /**
     * 关键词搜索报告：命中标题/医院/总结/指标名，限定自己家庭成员范围，最多返回 50 条
     *
     * @param keyword 关键词（空则返回空列表）
     */
    List<HealthReportVO> search(Long memberId, String keyword, Long familyMemberId);

    /**
     * 报告指标历年趋势（按报告年份升序，用于折线图）
     *
     * @param itemName 指标名（如 收缩压/空腹血糖）
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    List<TrendPointVO> trends(Long memberId, Long familyMemberId, String itemName);
}
