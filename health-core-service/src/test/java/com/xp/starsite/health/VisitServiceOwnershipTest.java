package com.xp.starsite.health;

import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.health.dto.VisitCreateDTO;
import com.xp.starsite.health.entity.VisitRecord;
import com.xp.starsite.health.mapper.VisitRecordMapper;
import com.xp.starsite.health.service.impl.VisitServiceImpl;
import com.xp.starsite.health.vo.PageVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 就医记录服务越权防线单测：归属校验失败时拒绝读写（mock 全部依赖，不连库）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class VisitServiceOwnershipTest {

    private static final long ME = 1L;
    private static final long MY_FAMILY_MEMBER = 11L;
    private static final long FOREIGN_FAMILY_MEMBER = 99L;

    @Mock
    private VisitRecordMapper visitMapper;
    @Mock
    private FamilyOwnershipGuard ownershipGuard;

    private VisitServiceImpl newService() {
        return new VisitServiceImpl(visitMapper, ownershipGuard);
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
}
