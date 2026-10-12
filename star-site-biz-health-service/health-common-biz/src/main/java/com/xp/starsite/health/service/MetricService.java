package com.xp.starsite.health.service;

import com.xp.starsite.health.dto.MetricBatchDTO;
import com.xp.starsite.health.vo.MetricTrendPoint;
import com.xp.starsite.health.vo.MetricVO;
import com.xp.starsite.health.vo.PageVO;

import java.util.List;

/**
 * 日常指标服务：血压/血糖/体重等自测指标的录入、查询、删除与趋势曲线。
 * 与报告指标（{@link ReportService}）的数据源不同：本服务读 daily_metric（按日自测），
 * 报告指标读 health_indicator（体检报告单）。
 * 归属防线约定同 {@link ReportService}（首参 memberId + family 归属校验，fail-closed）。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface MetricService {

    /**
     * 批量录入日常指标（血压一次产生高压+低压两条记录）
     *
     * @param memberId 当前登录账号
     * @param dto      指标批次（类型/数值/测量时间等）
     * @return 落库后的指标视图列表（含 id，供前端回填）
     * @throws com.xp.starsite.common.exception.BizException 403 批次内任一家庭成员不归属当前账号
     */
    List<MetricVO> addMetrics(Long memberId, MetricBatchDTO dto);

    /**
     * 日常指标分页列表（按测量日期倒序）
     *
     * @param memberId       当前登录账号
     * @param familyMemberId 可选，指定成员则单查（校验归属）；不传=全家范围
     * @param metricType     可选，指标类型过滤（bp_sys/bp_dia/weight/glucose...）
     * @param page           页码（1 起）
     * @param size           页大小
     * @return 指标分页
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    PageVO<MetricVO> listMetrics(Long memberId, Long familyMemberId, String metricType, long page, long size);

    /**
     * 删除日常指标（逻辑删，先校验归属）
     *
     * @param memberId 当前登录账号
     * @param id       记录 id
     * @throws com.xp.starsite.common.exception.BizException 400 记录不存在；403 非本人家庭成员的记录
     */
    void deleteMetric(Long memberId, Long id);

    /**
     * 日常指标趋势（按测量日期升序，孕期体重曲线等场景）
     *
     * @param memberId       当前登录账号
     * @param familyMemberId 目标家庭成员
     * @param metricType     指标类型（bp_sys/bp_dia/weight/glucose...）
     * @return 趋势点列表（测量时间升序）
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    List<MetricTrendPoint> metricTrends(Long memberId, Long familyMemberId, String metricType);
}
