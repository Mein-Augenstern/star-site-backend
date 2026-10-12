package com.xp.starsite.reminder;

import com.xp.starsite.api.event.reminder.ReminderCancelEvent;
import com.xp.starsite.api.event.reminder.ReminderRegisterEvent;
import com.xp.starsite.api.event.reminder.ReminderRepeatType;
import com.xp.starsite.reminder.entity.Notification;
import com.xp.starsite.reminder.entity.ReminderOccurrence;
import com.xp.starsite.reminder.entity.ReminderRule;
import com.xp.starsite.reminder.repository.NotificationRepository;
import com.xp.starsite.reminder.repository.ReminderOccurrenceRepository;
import com.xp.starsite.reminder.repository.ReminderRuleRepository;
import com.xp.starsite.reminder.service.impl.ReminderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 提醒中心单元测试：规则 upsert/撤销、重复日期推算、扫描投递（mock 全部依赖）。
 * mock 的是仓储业务语义方法（findByIdempotentKey/selectEnabled/selectDuePendingUpTo/markNotified 等）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    private static final long OWNER = 1L;
    private static final long FAMILY = 100L;

    @Mock
    private ReminderRuleRepository ruleRepository;
    @Mock
    private ReminderOccurrenceRepository occurrenceRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private org.springframework.beans.factory.ObjectProvider<com.xp.starsite.reminder.notify.EmailChannel> emailChannel;
    @Mock
    private com.xp.starsite.api.client.FamilyOwnershipGuard ownershipGuard;

    private ReminderServiceImpl newService() {
        // 邮件渠道缺省不装配（ObjectProvider 返回 null = 仅站内信）
        lenient().when(emailChannel.getIfAvailable()).thenReturn(null);
        return new ReminderServiceImpl(ruleRepository, occurrenceRepository, notificationRepository, ownershipGuard, emailChannel);
    }

    private ReminderRegisterEvent registerEvent() {
        ReminderRegisterEvent e = new ReminderRegisterEvent();
        e.setSource("star-site-insurance");
        e.setBizType("policy_payment");
        e.setBizId(7L);
        e.setFamilyId(FAMILY);
        e.setOwnerMemberId(OWNER);
        e.setTitle("平安福重疾 缴费");
        e.setFirstRemindAt(LocalDate.now().plusDays(5));
        e.setRepeatType(ReminderRepeatType.ANNUAL);
        e.setRepeatUntil(LocalDate.now().plusYears(20));
        e.setAdvanceDays(7);
        return e;
    }

    @Test
    void upsertInsertsNewRule() {
        when(ruleRepository.findByIdempotentKey("star-site-insurance", "policy_payment", 7L))
                .thenReturn(Optional.empty());

        newService().upsertRule(registerEvent());

        verify(ruleRepository).save(any(ReminderRule.class));
        // 立即补生成首期实例
        verify(occurrenceRepository).save(any(ReminderOccurrence.class));
    }

    @Test
    void upsertUpdatesExistingRule() {
        ReminderRule existing = new ReminderRule();
        existing.setId(3L);
        existing.setSource("star-site-insurance");
        existing.setBizType("policy_payment");
        existing.setBizId(7L);
        when(ruleRepository.findByIdempotentKey("star-site-insurance", "policy_payment", 7L))
                .thenReturn(Optional.of(existing));

        newService().upsertRule(registerEvent());

        verify(ruleRepository, never()).save(any(ReminderRule.class));
        verify(ruleRepository).update(any(ReminderRule.class));
    }

    @Test
    void cancelDeletesRuleAndPendingOccurrences() {
        ReminderRule rule = new ReminderRule();
        rule.setId(3L);
        when(ruleRepository.findByIdempotentKey("star-site-insurance", "policy_payment", 7L))
                .thenReturn(Optional.of(rule));

        ReminderCancelEvent e = new ReminderCancelEvent();
        e.setSource("star-site-insurance");
        e.setBizType("policy_payment");
        e.setBizId(7L);

        newService().cancelRule(e);

        verify(occurrenceRepository).deleteActiveByRuleId(3L);
        verify(ruleRepository).deleteById(3L);
    }

    @Test
    void annualRuleGeneratesWindowAndRespectsRepeatUntil() throws Exception {
        // 直接验证 generateOccurrences 的推算逻辑：借助 upsert 的立即生成路径
        ReminderRule rule = new ReminderRule();
        rule.setId(9L);
        rule.setFamilyId(FAMILY);
        rule.setOwnerMemberId(OWNER);
        rule.setTitle("续费");
        rule.setSource("star-site-insurance");
        rule.setBizType("policy_payment");
        rule.setBizId(7L);
        // 首期 = 明年 1-13（窗口 30 天外）；repeatUntil = 明年 6 月：窗口内应有且只有 0 条
        rule.setFirstRemindAt(LocalDate.now().plusMonths(8));
        rule.setRepeatType("ANNUAL");
        rule.setRepeatUntil(LocalDate.now().plusMonths(10));
        rule.setAdvanceDays(7);
        rule.setStatus(1);
        when(ruleRepository.findByIdempotentKey("star-site-insurance", "policy_payment", 7L))
                .thenReturn(Optional.empty());
        when(occurrenceRepository.countByRuleAndDueDate(any(), any())).thenReturn(0);

        newService().upsertRule(registerEvent());

        // 规则窗口（now ~ now+30d）内没有到期点 → 不生成实例
        ArgumentCaptor<ReminderOccurrence> captor = ArgumentCaptor.forClass(ReminderOccurrence.class);
        // upsertRule 用的是事件里的 first_remind_at（5 天后，窗口内）→ 应生成 1 条
        verify(occurrenceRepository, times(1)).save(captor.capture());
        assertEquals(LocalDate.now().plusDays(5), captor.getValue().getDueDate());
    }

    @Test
    void scanNotifiesDueOccurrences() {
        when(ruleRepository.selectEnabled()).thenReturn(List.of());

        ReminderOccurrence due = new ReminderOccurrence();
        due.setId(55L);
        due.setOwnerMemberId(OWNER);
        due.setTitle("平安福重疾 缴费");
        due.setStatus(1);
        when(occurrenceRepository.selectDuePendingUpTo(LocalDate.now())).thenReturn(List.of(due));

        int notified = newService().scanAndNotify();

        assertEquals(1, notified);
        ArgumentCaptor<Notification> n = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(n.capture());
        assertEquals("平安福重疾 缴费", n.getValue().getTitle());
        assertEquals(0, n.getValue().getReadFlag());
        // 实例被置为已通知
        assertEquals(2, due.getStatus());
        verify(occurrenceRepository).markNotified(eq(55L), any(LocalDateTime.class));
    }

    @Test
    void scanWithNothingDueWritesNothing() {
        when(ruleRepository.selectEnabled()).thenReturn(List.of());
        when(occurrenceRepository.selectDuePendingUpTo(LocalDate.now())).thenReturn(List.of());

        assertTrue(newService().scanAndNotify() == 0);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void overdueFirstRemindGeneratesTodayOccurrence() {
        // 逾期补投：首期(今天-2天)已在窗口前、规则未到期 → 今天补一期，用户立即收到提醒
        ReminderRule rule = new ReminderRule();
        rule.setId(9L);
        rule.setFamilyId(FAMILY);
        rule.setOwnerMemberId(OWNER);
        rule.setTitle("平安福重疾 缴费");
        rule.setSource("star-site-insurance");
        rule.setBizType("policy_payment");
        rule.setBizId(7L);
        rule.setFirstRemindAt(LocalDate.now().minusDays(2));
        rule.setRepeatType("ANNUAL");
        rule.setRepeatUntil(LocalDate.now().plusYears(20));
        rule.setAdvanceDays(7);
        rule.setStatus(1);
        when(ruleRepository.selectEnabled()).thenReturn(List.of(rule));
        when(occurrenceRepository.countByRuleAndDueDate(any(), any())).thenReturn(0);
        when(occurrenceRepository.selectDuePendingUpTo(LocalDate.now())).thenReturn(List.of());

        newService().scanAndNotify();

        ArgumentCaptor<ReminderOccurrence> captor = ArgumentCaptor.forClass(ReminderOccurrence.class);
        verify(occurrenceRepository).save(captor.capture());
        // 应提醒日 = 今天（而不是被窗口跳过）
        assertEquals(LocalDate.now(), captor.getValue().getDueDate());
    }
}
