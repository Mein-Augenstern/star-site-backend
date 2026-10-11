package com.xp.starsite.insurance;

import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.mq.EventPublisher;
import com.xp.starsite.insurance.dto.PolicySaveDTO;
import com.xp.starsite.insurance.entity.InsurancePolicy;
import com.xp.starsite.insurance.mapper.InsurancePolicyMapper;
import com.xp.starsite.insurance.service.impl.InsuranceServiceImpl;
import com.xp.starsite.insurance.vo.PageVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 保单服务单元测试：越权防线 + 提醒事件载荷（mock 全部依赖，不连库不连 MQ）
 */
@ExtendWith(MockitoExtension.class)
class InsuranceServiceTest {

    private static final long ME = 1L;
    private static final long MY_MEMBER = 11L;
    private static final long FOREIGN_MEMBER = 99L;

    @Mock
    private InsurancePolicyMapper policyMapper;
    @Mock
    private FamilyOwnershipGuard ownershipGuard;
    /** EventPublisher 是接口，mock 后可捕获事件载荷断言 */
    @Mock
    private EventPublisher eventPublisher;

    private InsuranceServiceImpl newService() {
        return new InsuranceServiceImpl(policyMapper, ownershipGuard, eventPublisher);
    }

    private PolicySaveDTO dto(long insuredMemberId) {
        PolicySaveDTO dto = new PolicySaveDTO();
        dto.setCompany("平安人寿");
        dto.setProductName("平安福重疾");
        dto.setCategory("critical_illness");
        dto.setInsuredMemberId(insuredMemberId);
        dto.setPremium(new BigDecimal("8000"));
        dto.setPaymentFreq("annual");
        dto.setNextPaymentDate(LocalDate.of(2027, 1, 20));
        dto.setMaturityDate(LocalDate.of(2046, 1, 20));
        return dto;
    }

    private FamilyMemberSnapshotDTO snapshot(long id) {
        FamilyMemberSnapshotDTO s = new FamilyMemberSnapshotDTO();
        s.setId(id);
        s.setFamilyId(100L);
        s.setName("张三");
        s.setRelation("self");
        return s;
    }

    @Test
    void createRejectsForeignInsuredMember() {
        when(ownershipGuard.requireOwned(ME, FOREIGN_MEMBER))
                .thenThrow(new BizException(403, "无权限访问该家庭成员的数据"));

        BizException e = assertThrows(BizException.class,
                () -> newService().create(ME, dto(FOREIGN_MEMBER)));
        assertEquals(403, e.getCode());
        verify(policyMapper, never()).insert(any(InsurancePolicy.class));
    }

    @Test
    void createPublishesTransactionalPaymentReminder() {
        when(ownershipGuard.requireOwned(ME, MY_MEMBER)).thenReturn(snapshot(MY_MEMBER));
        // 模拟事务消息语义：本地事务回调真实执行（insert 发生），之后可按 id 回读
        org.mockito.Mockito.doAnswer(inv -> {
            ((com.xp.starsite.common.mq.LocalTransactionExecutor) inv.getArgument(5)).execute();
            return null;
        }).when(eventPublisher).publishTransactional(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        java.util.concurrent.atomic.AtomicReference<InsurancePolicy> inserted =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(policyMapper.insert(any(InsurancePolicy.class))).thenAnswer(inv -> {
            inserted.set(inv.getArgument(0));
            return 1;
        });
        when(policyMapper.selectById(any(Long.class))).thenAnswer(inv -> inserted.get());

        newService().create(ME, dto(MY_MEMBER));

        // 事务消息：载荷是缴费提醒，首次提醒 = 缴费日 - 7 天，重复至满期
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishTransactional(
                eq("TOPIC_REMINDER"), eq("ReminderRegister"), eq("policy"),
                any(Long.class), payload.capture(), any());
        com.xp.starsite.api.event.reminder.ReminderRegisterEvent evt =
                (com.xp.starsite.api.event.reminder.ReminderRegisterEvent) payload.getValue();
        assertEquals("policy_payment", evt.getBizType());
        assertEquals(LocalDate.of(2027, 1, 13), evt.getFirstRemindAt());
        assertEquals(com.xp.starsite.api.event.reminder.ReminderRepeatType.ANNUAL, evt.getRepeatType());
        assertEquals(LocalDate.of(2046, 1, 20), evt.getRepeatUntil());
        // 落库在事务回调里执行
        verify(policyMapper).insert(any(InsurancePolicy.class));
    }

    @Test
    void detailRejectsForeignPolicy() {
        InsurancePolicy foreign = new InsurancePolicy();
        foreign.setId(7L);
        foreign.setOwnerMemberId(2L);
        when(policyMapper.selectById(7L)).thenReturn(foreign);

        BizException e = assertThrows(BizException.class, () -> newService().getDetail(ME, 7L));
        assertEquals(403, e.getCode());
    }

    @Test
    void deleteCancelsBothReminders() {
        InsurancePolicy mine = new InsurancePolicy();
        mine.setId(7L);
        mine.setOwnerMemberId(ME);
        mine.setProductName("平安福重疾");
        when(policyMapper.selectById(7L)).thenReturn(mine);

        newService().delete(ME, 7L);

        verify(policyMapper).deleteById(7L);
        ArgumentCaptor<Object> payloads = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publish(
                eq("TOPIC_REMINDER"), eq("ReminderCancel"), eq("policy"), eq(7L), payloads.capture());
        List<Object> events = payloads.getAllValues();
        assertEquals("policy_payment", ((com.xp.starsite.api.event.reminder.ReminderCancelEvent) events.get(0)).getBizType());
        assertEquals("policy_expiry", ((com.xp.starsite.api.event.reminder.ReminderCancelEvent) events.get(1)).getBizType());
    }

    @Test
    void listNarrowsToOwner() {
        when(policyMapper.selectPage(any(), any())).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        PageVO<?> page = newService().list(ME, null, "critical_illness", 1, 1, 10);
        assertEquals(0, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }
}
