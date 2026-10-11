package com.xp.starsite.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.health.dto.MetricBatchDTO;
import com.xp.starsite.health.entity.DailyMetric;
import com.xp.starsite.health.mapper.DailyMetricMapper;
import com.xp.starsite.health.service.MetricService;
import com.xp.starsite.health.vo.MetricTrendPoint;
import com.xp.starsite.health.vo.MetricVO;
import com.xp.starsite.health.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 日常指标服务实现。越权防线约定见 {@link com.xp.starsite.health.service.MetricService}。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricServiceImpl implements MetricService {

    private final DailyMetricMapper metricMapper;
    private final FamilyOwnershipGuard ownershipGuard;

    @Override
    public List<MetricVO> addMetrics(Long memberId, MetricBatchDTO dto) {
        // 先整批校验归属（任一不归属整批拒绝），再逐条落库
        for (MetricBatchDTO.Item item : dto.getItems()) {
            ownershipGuard.requireOwned(memberId, item.getFamilyMemberId());
        }
        List<MetricVO> result = new ArrayList<>();
        for (MetricBatchDTO.Item item : dto.getItems()) {
            DailyMetric m = new DailyMetric();
            m.setFamilyMemberId(item.getFamilyMemberId());
            m.setMetricDate(item.getMetricDate());
            m.setMetricType(item.getMetricType());
            m.setValueNum(item.getValueNum());
            m.setRemark(item.getRemark());
            metricMapper.insert(m);
            result.add(toVO(m));
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
        List<MetricVO> vos = result.getRecords().stream().map(this::toVO).toList();
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
    public List<MetricTrendPoint> metricTrends(Long memberId, Long familyMemberId, String metricType) {
        ownershipGuard.requireOwned(memberId, familyMemberId);
        return metricMapper.selectList(
                        new LambdaQueryWrapper<DailyMetric>()
                                .eq(DailyMetric::getFamilyMemberId, familyMemberId)
                                .eq(DailyMetric::getMetricType, metricType)
                                .orderByAsc(DailyMetric::getMetricDate))
                .stream()
                .map(m -> new MetricTrendPoint(m.getMetricDate(), m.getValueNum()))
                .toList();
    }

    /** 列表/搜索的数据范围（指定成员单查，否则全家收口），与报告服务同语义 */
    private Set<Long> ownedScope(Long memberId, Long familyMemberId) {
        if (familyMemberId != null) {
            ownershipGuard.requireOwned(memberId, familyMemberId);
            return Set.of(familyMemberId);
        }
        return Set.copyOf(ownershipGuard.listOwnedMemberIds(memberId));
    }

    private MetricVO toVO(DailyMetric m) {
        MetricVO vo = new MetricVO();
        BeanUtils.copyProperties(m, vo);
        return vo;
    }
}
