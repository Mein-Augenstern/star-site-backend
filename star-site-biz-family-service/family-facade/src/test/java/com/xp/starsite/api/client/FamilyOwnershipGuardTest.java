package com.xp.starsite.api.client;

import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.R;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 家庭归属防线单测：验证 FamilyOwnershipGuard 归属放行/越权 403/服务不可用 fail-closed 500
 * 三态语义（不启 Spring），以及成员范围列表的空家庭兜底。
 *
 * @author xiaopu
 * @since 2026-10
 */
class FamilyOwnershipGuardTest {

    private final FamilyApi familyApi = mock(FamilyApi.class);
    private final FamilyOwnershipGuard guard = new FamilyOwnershipGuard(familyApi);

    private static FamilyMemberSnapshotDTO snapshot(long id) {
        FamilyMemberSnapshotDTO dto = new FamilyMemberSnapshotDTO();
        dto.setId(id);
        dto.setFamilyId(100L);
        dto.setName("张三");
        return dto;
    }

    @Test
    void ownedMemberReturnsSnapshot() {
        when(familyApi.checkOwnership(1L, 11L)).thenReturn(R.ok(snapshot(11L)));
        assertEquals("张三", guard.requireOwned(1L, 11L).getName());
    }

    @Test
    void foreignMemberThrows403() {
        // 不归属：data=null
        when(familyApi.checkOwnership(1L, 99L)).thenReturn(R.build(0, "成功", null));
        BizException e = assertThrows(BizException.class, () -> guard.requireOwned(1L, 99L));
        assertEquals(403, e.getCode());
    }

    @Test
    void familyServiceDownFailsClosed() {
        when(familyApi.checkOwnership(anyLong(), anyLong())).thenThrow(new RuntimeException("connect timeout"));
        BizException e = assertThrows(BizException.class, () -> guard.requireOwned(1L, 11L));
        // fail-closed：服务不可用拒绝请求（500），绝不放行
        assertEquals(500, e.getCode());
    }

    @Test
    void listOwnedIdsEmptyFamily() {
        when(familyApi.listMembers(1L)).thenReturn(R.build(0, "成功", null));
        assertTrue(guard.listOwnedMemberIds(1L).isEmpty());
    }

    @Test
    void listOwnedIdsNormal() {
        when(familyApi.listMembers(1L)).thenReturn(R.ok(List.of(snapshot(11L), snapshot(12L))));
        assertEquals(List.of(11L, 12L), guard.listOwnedMemberIds(1L));
    }
}
