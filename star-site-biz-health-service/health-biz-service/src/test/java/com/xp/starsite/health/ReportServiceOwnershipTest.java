package com.xp.starsite.health;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.health.dto.ReportCreateDTO;
import com.xp.starsite.health.entity.HealthIndicator;
import com.xp.starsite.health.entity.HealthReport;
import com.xp.starsite.health.mapper.HealthIndicatorMapper;
import com.xp.starsite.health.mapper.HealthReportMapper;
import com.xp.starsite.health.service.impl.ReportServiceImpl;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报告服务越权防线单测：归属校验失败时拒绝读写（mock 全部依赖，不连库）。
 * 覆盖：建档/详情/列表收口/删除四条主链路的 403/400 分支。
 *
 * @author xiaopu
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceOwnershipTest {

    private static final long ME = 1L;
    private static final long MY_FAMILY_MEMBER = 11L;
    private static final long FOREIGN_FAMILY_MEMBER = 99L;

    @Mock
    private HealthReportMapper reportMapper;
    @Mock
    private HealthIndicatorMapper indicatorMapper;
    @Mock
    private FamilyOwnershipGuard ownershipGuard;

    private ReportServiceImpl newService() {
        return new ReportServiceImpl(reportMapper, indicatorMapper, ownershipGuard);
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
}
