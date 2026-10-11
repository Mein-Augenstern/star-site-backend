package com.xp.starsite.health;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.health.dto.MetricBatchDTO;
import com.xp.starsite.health.dto.ReportCreateDTO;
import com.xp.starsite.health.dto.VisitCreateDTO;
import com.xp.starsite.health.entity.DailyMetric;
import com.xp.starsite.health.entity.HealthIndicator;
import com.xp.starsite.health.entity.HealthReport;
import com.xp.starsite.health.entity.VisitRecord;
import com.xp.starsite.health.mapper.DailyMetricMapper;
import com.xp.starsite.health.mapper.HealthIndicatorMapper;
import com.xp.starsite.health.mapper.HealthReportMapper;
import com.xp.starsite.health.mapper.VisitRecordMapper;
import com.xp.starsite.health.service.impl.HealthServiceImpl;
import com.xp.starsite.health.vo.PageVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 越权防线单元测试：验证 service 层在归属校验失败时拒绝读写（mock 全部依赖，不连库）
 */
@ExtendWith(MockitoExtension.class)
class HealthServiceOwnershipTest {

    private static final long ME = 1L;
    private static final long MY_FAMILY_MEMBER = 11L;
    private static final long FOREIGN_FAMILY_MEMBER = 99L;

    @Mock
    private HealthReportMapper reportMapper;
    @Mock
    private HealthIndicatorMapper indicatorMapper;
    @Mock
    private VisitRecordMapper visitMapper;
    @Mock
    private DailyMetricMapper metricMapper;
    @Mock
    private com.xp.starsite.health.mapper.PregnancyProfileMapper pregnancyMapper;
    @Mock
    private FamilyOwnershipGuard ownershipGuard;

    private HealthServiceImpl newService() {
        return new HealthServiceImpl(reportMapper, indicatorMapper, visitMapper, metricMapper, pregnancyMapper, ownershipGuard);
    }

    private HealthReport report(long id, long familyMemberId) {
        HealthReport r = new HealthReport();
        r.setId(id);
        r.setFamilyMemberId(familyMemberId);
        r.setReportYear(2026);
        return r;
    }

    @Test
    void createRejectsForeignFamilyMember() {
        when(ownershipGuard.requireOwned(ME, FOREIGN_FAMILY_MEMBER))
                .thenThrow(new BizException(403, "无权限访问该家庭成员的数据"));

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setFamilyMemberId(FOREIGN_FAMILY_MEMBER);
        dto.setReportYear(2026);

        BizException e = assertThrows(BizException.class,
                () -> newService().createReport(ME, dto));
        assertEquals(403, e.getCode());
        verify(ownershipGuard).requireOwned(ME, FOREIGN_FAMILY_MEMBER);
    }

    @Test
    void detailRejectsForeignReport() {
        // 报告属于别人的家庭成员：归属校验必须被触发
        when(reportMapper.selectById(7L)).thenReturn(report(7L, FOREIGN_FAMILY_MEMBER));
        when(ownershipGuard.requireOwned(ME, FOREIGN_FAMILY_MEMBER))
                .thenThrow(new BizException(403, "无权限访问该家庭成员的数据"));

        BizException e = assertThrows(BizException.class, () -> newService().getDetail(ME, 7L));
        assertEquals(403, e.getCode());
    }

    @Test
    void detailAllowsOwnReport() {
        when(reportMapper.selectById(7L)).thenReturn(report(7L, MY_FAMILY_MEMBER));
        when(ownershipGuard.requireOwned(ME, MY_FAMILY_MEMBER)).thenReturn(null);
        when(indicatorMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<HealthIndicator>>any()))
                .thenReturn(List.of());

        assertEquals(7L, newService().getDetail(ME, 7L).getId());
    }

    @Test
    void listWithoutMemberIdNarrowsToEmptyFamily() {
        // 未建家庭（ownedIds 空）：直接空页，不打数据库
        when(ownershipGuard.listOwnedMemberIds(ME)).thenReturn(List.of());

        PageVO<?> page = newService().listReports(ME, null, null, 1, 10);
        assertEquals(0, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void deleteRejectsForeignReport() {
        when(reportMapper.selectById(8L)).thenReturn(report(8L, FOREIGN_FAMILY_MEMBER));
        when(ownershipGuard.requireOwned(ME, FOREIGN_FAMILY_MEMBER))
                .thenThrow(new BizException(403, "无权限访问该家庭成员的数据"));

        assertThrows(BizException.class, () -> newService().deleteReport(ME, 8L));
        verify(reportMapper).selectById(8L);
    }

    private VisitCreateDTO visitDto(long familyMemberId) {
        VisitCreateDTO dto = new VisitCreateDTO();
        dto.setFamilyMemberId(familyMemberId);
        dto.setVisitDate(java.time.LocalDate.of(2026, 10, 1));
        dto.setHospital("市第一人民医院");
        return dto;
    }

    @Test
    void createVisitRejectsForeignFamilyMember() {
        when(ownershipGuard.requireOwned(ME, FOREIGN_FAMILY_MEMBER))
                .thenThrow(new BizException(403, "无权限访问该家庭成员的数据"));

        BizException e = assertThrows(BizException.class,
                () -> newService().createVisit(ME, visitDto(FOREIGN_FAMILY_MEMBER)));
        assertEquals(403, e.getCode());
    }

    @Test
    void createVisitAllowsOwnFamilyMember() {
        when(ownershipGuard.requireOwned(ME, MY_FAMILY_MEMBER)).thenReturn(null);
        when(visitMapper.insert(any(VisitRecord.class))).thenAnswer(inv -> {
            inv.getArgument(0, VisitRecord.class).setId(1L);
            return 1;
        });

        assertEquals(1L, newService().createVisit(ME, visitDto(MY_FAMILY_MEMBER)).getId());
    }

    @Test
    void listVisitsWithoutMemberIdNarrowsToEmptyFamily() {
        when(ownershipGuard.listOwnedMemberIds(ME)).thenReturn(List.of());

        PageVO<?> page = newService().listVisits(ME, null, 1, 10);
        assertEquals(0, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void deleteVisitRejectsForeignRecord() {
        VisitRecord foreign = new VisitRecord();
        foreign.setId(9L);
        foreign.setFamilyMemberId(FOREIGN_FAMILY_MEMBER);
        when(visitMapper.selectById(9L)).thenReturn(foreign);
        when(ownershipGuard.requireOwned(ME, FOREIGN_FAMILY_MEMBER))
                .thenThrow(new BizException(403, "无权限访问该家庭成员的数据"));

        assertThrows(BizException.class, () -> newService().deleteVisit(ME, 9L));
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
    }
}
