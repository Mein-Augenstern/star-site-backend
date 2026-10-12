package com.xp.starsite.health;

import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.health.dto.MetricBatchDTO;
import com.xp.starsite.health.entity.DailyMetric;
import com.xp.starsite.health.mapper.DailyMetricMapper;
import com.xp.starsite.health.service.impl.MetricServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 日常指标服务越权防线单测：批次内任一成员不归属整批拒绝（mock 全部依赖，不连库）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class MetricServiceOwnershipTest {

    private static final long ME = 1L;
    private static final long FOREIGN_FAMILY_MEMBER = 99L;

    @Mock
    private DailyMetricMapper metricMapper;
    @Mock
    private FamilyOwnershipGuard ownershipGuard;

    private MetricServiceImpl newService() {
        return new MetricServiceImpl(metricMapper, ownershipGuard);
    }

    @Test
    void addMetricsRejectsForeignFamilyMember() {
        MetricBatchDTO dto = new MetricBatchDTO();
        MetricBatchDTO.Item item = new MetricBatchDTO.Item();
        item.setFamilyMemberId(FOREIGN_FAMILY_MEMBER);
        item.setMetricDate(java.time.LocalDate.of(2026, 10, 10));
        item.setMetricType("bp_sys");
        item.setValueNum(java.math.BigDecimal.valueOf(135));
        dto.setItems(java.util.List.of(item));
        when(ownershipGuard.requireOwned(ME, FOREIGN_FAMILY_MEMBER))
                .thenThrow(new BizException(403, "无权限访问该家庭成员的数据"));

        BizException e = assertThrows(BizException.class, () -> newService().addMetrics(ME, dto));
        assertEquals(403, e.getCode());
    }

    @Test
    void deleteMetricRejectsForeignRecord() {
        DailyMetric foreign = new DailyMetric();
        foreign.setId(20L);
        foreign.setFamilyMemberId(FOREIGN_FAMILY_MEMBER);
        when(metricMapper.selectById(20L)).thenReturn(foreign);
        when(ownershipGuard.requireOwned(ME, FOREIGN_FAMILY_MEMBER))
                .thenThrow(new BizException(403, "无权限访问该家庭成员的数据"));

        assertThrows(BizException.class, () -> newService().deleteMetric(ME, 20L));
        verify(metricMapper).selectById(20L);
    }
}
