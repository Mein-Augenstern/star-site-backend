package com.xp.starsite.archive;

import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.mq.EventPublisher;
import com.xp.starsite.archive.dto.AssetSaveDTO;
import com.xp.starsite.archive.dto.DocumentSaveDTO;
import com.xp.starsite.archive.entity.ArchiveAsset;
import com.xp.starsite.archive.entity.AssetValuation;
import com.xp.starsite.archive.entity.ArchiveDocument;
import com.xp.starsite.archive.mapper.ArchiveAssetMapper;
import com.xp.starsite.archive.mapper.ArchiveDocumentMapper;
import com.xp.starsite.archive.mapper.AssetValuationMapper;
import com.xp.starsite.archive.service.impl.AssetServiceImpl;
import com.xp.starsite.archive.service.impl.DocumentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 家庭档案单元测试：归属防线 + 提醒事件载荷 + 估值留痕（mock 全部依赖）
 *
 * @author xiaopu
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    private static final long ME = 1L;
    private static final long MY_MEMBER = 11L;
    private static final long FOREIGN_MEMBER = 99L;

    @Mock
    private ArchiveDocumentMapper documentMapper;
    @Mock
    private ArchiveAssetMapper assetMapper;
    @Mock
    private AssetValuationMapper valuationMapper;
    @Mock
    private FamilyOwnershipGuard ownershipGuard;
    @Mock
    private EventPublisher eventPublisher;

    private FamilyMemberSnapshotDTO snapshot(long id) {
        FamilyMemberSnapshotDTO s = new FamilyMemberSnapshotDTO();
        s.setId(id);
        s.setFamilyId(100L);
        s.setName("张三");
        s.setRelation("self");
        return s;
    }

    /* ===== 证件域 ===== */

    private DocumentSaveDTO docDto(long holder) {
        DocumentSaveDTO dto = new DocumentSaveDTO();
        dto.setDocType("id_card");
        dto.setHolderMemberId(holder);
        dto.setDocNo("110101199001011234");
        dto.setExpiryDate(LocalDate.of(2030, 6, 15));
        return dto;
    }

    private void mockTxAndReadback() {
        doAnswer(inv -> {
            ((com.xp.starsite.common.mq.LocalTransactionExecutor) inv.getArgument(5)).execute();
            return null;
        }).when(eventPublisher).publishTransactional(anyString(), anyString(), anyString(), any(), any(), any());
        AtomicReference<ArchiveDocument> inserted = new AtomicReference<>();
        when(documentMapper.insert(any(ArchiveDocument.class))).thenAnswer(inv -> {
            inserted.set(inv.getArgument(0));
            return 1;
        });
        when(documentMapper.selectById(anyLong())).thenAnswer(inv -> inserted.get());
    }

    @Test
    void documentRejectsForeignHolder() {
        when(ownershipGuard.requireOwned(ME, FOREIGN_MEMBER))
                .thenThrow(new BizException(403, "无权限访问该家庭成员的数据"));

        BizException e = assertThrows(BizException.class,
                () -> new DocumentServiceImpl(documentMapper, ownershipGuard, eventPublisher)
                        .create(ME, docDto(FOREIGN_MEMBER)));
        assertEquals(403, e.getCode());
        verify(documentMapper, never()).insert(any(ArchiveDocument.class));
    }

    @Test
    void documentCreatePublishesExpiryReminder() {
        when(ownershipGuard.requireOwned(ME, MY_MEMBER)).thenReturn(snapshot(MY_MEMBER));
        mockTxAndReadback();

        new DocumentServiceImpl(documentMapper, ownershipGuard, eventPublisher).create(ME, docDto(MY_MEMBER));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishTransactional(
                eq("TOPIC_REMINDER"), eq("ReminderRegister"), eq("document"),
                any(Long.class), payload.capture(), any());
        com.xp.starsite.api.event.reminder.ReminderRegisterEvent evt =
                (com.xp.starsite.api.event.reminder.ReminderRegisterEvent) payload.getValue();
        assertEquals("document_expiry", evt.getBizType());
        // 到期前 30 天提醒
        assertEquals(LocalDate.of(2030, 5, 16), evt.getFirstRemindAt());
        assertEquals(com.xp.starsite.api.event.reminder.ReminderRepeatType.NONE, evt.getRepeatType());
    }

    /* ===== 资产域 ===== */

    private AssetSaveDTO assetDto(String type, LocalDate inspectDate) {
        AssetSaveDTO dto = new AssetSaveDTO();
        dto.setName("特斯拉 Model Y");
        dto.setAssetType(type);
        dto.setHolderMemberId(MY_MEMBER);
        dto.setCurrentValue(new BigDecimal("230000"));
        dto.setNextInspectDate(inspectDate);
        return dto;
    }

    @Test
    void vehicleWithInspectDateSendsTransactionalReminder() {
        when(ownershipGuard.requireOwned(ME, MY_MEMBER)).thenReturn(snapshot(MY_MEMBER));
        // 事务消息回调真实执行 + 按 id 回读
        doAnswer(inv -> {
            ((com.xp.starsite.common.mq.LocalTransactionExecutor) inv.getArgument(5)).execute();
            return null;
        }).when(eventPublisher).publishTransactional(anyString(), anyString(), anyString(), any(), any(), any());
        AtomicReference<ArchiveAsset> inserted = new AtomicReference<>();
        when(assetMapper.insert(any(ArchiveAsset.class))).thenAnswer(inv -> {
            inserted.set(inv.getArgument(0));
            return 1;
        });
        when(assetMapper.selectById(anyLong())).thenAnswer(inv -> inserted.get());

        new AssetServiceImpl(assetMapper, valuationMapper, ownershipGuard, eventPublisher)
                .create(ME, assetDto("vehicle", LocalDate.of(2027, 3, 10)));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishTransactional(
                eq("TOPIC_REMINDER"), eq("ReminderRegister"), eq("asset"),
                any(Long.class), payload.capture(), any());
        com.xp.starsite.api.event.reminder.ReminderRegisterEvent evt =
                (com.xp.starsite.api.event.reminder.ReminderRegisterEvent) payload.getValue();
        assertEquals("vehicle_inspect", evt.getBizType());
        // 年检前 15 天，逐年重复
        assertEquals(LocalDate.of(2027, 2, 23), evt.getFirstRemindAt());
        assertEquals(com.xp.starsite.api.event.reminder.ReminderRepeatType.ANNUAL, evt.getRepeatType());
        verify(assetMapper).insert(any(ArchiveAsset.class));
        // 首次估值留痕
        verify(valuationMapper).insert(any(AssetValuation.class));
    }

    @Test
    void depositWithoutInspectDateSkipsReminder() {
        when(ownershipGuard.requireOwned(ME, MY_MEMBER)).thenReturn(snapshot(MY_MEMBER));
        when(assetMapper.insert(any(ArchiveAsset.class))).thenReturn(1);
        when(assetMapper.selectById(any(Long.class))).thenAnswer(inv -> {
            ArchiveAsset a = new ArchiveAsset();
            a.setId(1L);
            a.setName("存款");
            a.setAssetType("deposit");
            return a;
        });

        new AssetServiceImpl(assetMapper, valuationMapper, ownershipGuard, eventPublisher)
                .create(ME, assetDto("deposit", null));

        verify(eventPublisher, never()).publishTransactional(any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void valuationChangeAppendsHistory() {
        ArchiveAsset existing = new ArchiveAsset();
        existing.setId(5L);
        existing.setOwnerMemberId(ME);
        existing.setName("老房子");
        existing.setAssetType("real_estate");
        existing.setCurrentValue(new BigDecimal("3000000"));
        existing.setValuedAt(LocalDate.of(2026, 1, 1).atStartOfDay());
        when(assetMapper.selectById(5L)).thenReturn(existing);

        new AssetServiceImpl(assetMapper, valuationMapper, ownershipGuard, eventPublisher)
                .update(ME, 5L, assetDto("real_estate", null));

        verify(assetMapper).updateById(any(ArchiveAsset.class));
        // 估值变更 → 追加一条历史
        verify(valuationMapper, times(1)).insert(any(AssetValuation.class));
    }

    @Test
    void assetRejectsForeignOwner() {
        ArchiveAsset foreign = new ArchiveAsset();
        foreign.setId(6L);
        foreign.setOwnerMemberId(2L);
        when(assetMapper.selectById(6L)).thenReturn(foreign);

        BizException e = assertThrows(BizException.class,
                () -> new AssetServiceImpl(assetMapper, valuationMapper, ownershipGuard, eventPublisher)
                        .getDetail(ME, 6L));
        assertEquals(403, e.getCode());
    }
}
