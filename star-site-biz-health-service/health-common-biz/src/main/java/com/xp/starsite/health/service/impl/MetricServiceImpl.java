package com.xp.starsite.health.service.impl;

import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.health.dto.MetricBatchDTO;
import com.xp.starsite.health.entity.DailyMetric;
import com.xp.starsite.health.repository.DailyMetricRepository;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 日常指标服务实现。越权防线约定见 {@link com.xp.starsite.health.service.MetricService}。
 * 数据访问经仓储层（{@link DailyMetricRepository}，实现位于 core-service、底层调用 dal DAO），
 * service 只面向领域模型 Entity 编排。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricServiceImpl implements MetricService {

    private final DailyMetricRepository metricRepository;
    private final FamilyOwnershipGuard ownershipGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
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
            metricRepository.save(m);
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
        String type = StringUtils.hasText(metricType) ? metricType : null;
        long total = metricRepository.countByScope(scope, type);
        if (total == 0) {
            return PageVO.of(List.of(), 0, page, size);
        }
        long offset = (page - 1) * size;
        List<MetricVO> vos = metricRepository.pageByScope(scope, type, offset, (int) size)
                .stream().map(this::toVO).toList();
        return PageVO.of(vos, total, page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMetric(Long memberId, Long id) {
        DailyMetric m = metricRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.BAD_REQUEST.getCode(), "指标记录不存在"));
        ownershipGuard.requireOwned(memberId, m.getFamilyMemberId());
        metricRepository.markDeleted(id);
    }

    @Override
    public List<MetricTrendPoint> metricTrends(Long memberId, Long familyMemberId, String metricType) {
        ownershipGuard.requireOwned(memberId, familyMemberId);
        return metricRepository.selectTrend(familyMemberId, metricType)
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
