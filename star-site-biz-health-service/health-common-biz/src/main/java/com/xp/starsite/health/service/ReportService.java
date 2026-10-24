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
 * @author muyi
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
     * @param memberId        当前登录账号
     * @param familyMemberId  可选，指定成员则单查（校验归属）；不传=全家范围
     * @param year            可选，按报告年份过滤
     * @param page            页码（1 起）
     * @param size            页大小
     * @return 报告分页（无家庭成员时为空页）
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    PageVO<HealthReportVO> listReports(Long memberId, Long familyMemberId, Integer year, long page, long size);

    /**
     * 报告详情（含全部指标，按 sortNo 排序）
     *
     * @param memberId 当前登录账号
     * @param id       报告 id
     * @return 报告视图（含指标全集）
     * @throws com.xp.starsite.common.exception.BizException 400 报告不存在；403 非本人家庭成员的报告
     */
    HealthReportVO getDetail(Long memberId, Long id);

    /**
     * 批量录入报告指标（重录覆盖语义：先清空该报告旧指标再全量插入；
     * 异常标志按参考区间自动判定，无需前端传入）
     *
     * @param memberId 当前登录账号
     * @param reportId 报告 id
     * @param dto      指标批次（名称/结果/参考区间等）
     * @throws com.xp.starsite.common.exception.BizException 400 报告不存在；403 非本人家庭成员的报告
     */
    void addIndicators(Long memberId, Long reportId, IndicatorBatchDTO dto);

    /**
     * 删除报告（逻辑删）
     *
     * @param memberId 当前登录账号
     * @param id       报告 id
     * @throws com.xp.starsite.common.exception.BizException 400 报告不存在；403 非本人家庭成员的报告
     */
    void deleteReport(Long memberId, Long id);

    /**
     * 关键词搜索报告：命中标题/医院/总结/指标名，限定自己家庭成员范围，最多返回 50 条
     *
     * @param memberId        当前登录账号
     * @param keyword         关键词（空则返回空列表）
     * @param familyMemberId  可选，指定成员则单查（校验归属）；不传=全家范围
     * @return 命中报告列表（按体检日期倒序）
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    List<HealthReportVO> search(Long memberId, String keyword, Long familyMemberId);

    /**
     * 报告详情（内部契约出参：facade DTO，含全部指标）。
     * 供 HealthInternalController（health 横向化只读契约）使用——
     * 对齐 family 先例：服务返回 facade DTO，控制器零映射。
     *
     * @param memberId 当前登录账号
     * @param reportId 报告 id
     * @throws com.xp.starsite.common.exception.BizException 400 报告不存在；403 非本人家庭成员的报告
     */
    com.xp.starsite.api.dto.health.HealthReportDTO getInternalDetail(Long memberId, Long reportId);

    /**
     * 报告摘要列表（内部契约出参：facade DTO，不含指标，按体检日期倒序，最近 20 份）。
     * 智能体工具「查全家最近报告」用。
     *
     * @param memberId        当前登录账号
     * @param familyMemberId  可选，指定成员则单查（校验归属）；不传=全家范围
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    java.util.List<com.xp.starsite.api.dto.health.HealthReportDTO> listInternal(Long memberId, Long familyMemberId);

    /**
     * 报告指标历年趋势（按报告年份升序，用于折线图）
     *
     * @param memberId       当前登录账号
     * @param familyMemberId 目标家庭成员
     * @param itemName       指标名（如 收缩压/空腹血糖）
     * @return 趋势点列表（年份升序）
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    List<TrendPointVO> trends(Long memberId, Long familyMemberId, String itemName);
}
