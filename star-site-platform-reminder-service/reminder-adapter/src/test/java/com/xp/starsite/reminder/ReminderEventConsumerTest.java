package com.xp.starsite.reminder;

import com.xp.starsite.api.event.reminder.ReminderRegisterEvent;
import com.xp.starsite.reminder.mq.ReminderEventConsumer;
import com.xp.starsite.reminder.service.ReminderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xp.starsite.common.mq.event.EventEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 提醒事件消费者测试：信封解码 + eventId 幂等分发
 *
 * @author xiaopu
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class ReminderEventConsumerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private ReminderService reminderService;

    private ReminderEventConsumer newConsumer(JdbcTemplate jdbcTemplate) {
        return new ReminderEventConsumer(jdbcTemplate, reminderService);
    }

    private String envelopeJson(String eventId, String type, Object payload) throws Exception {
        EventEnvelope env = EventEnvelope.builder()
                .eventId(eventId)
                .eventType(type)
                .sourceService("star-site-insurance")
                .bizType("policy")
                .bizId(7L)
                .occurredAt(LocalDate.now().atStartOfDay())
                .payloadJson(MAPPER.writeValueAsString(payload))
                .build();
        return MAPPER.writeValueAsString(env);
    }

    @Test
    void registerEventDispatchedOnce() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), anyString(), eq("evt-1"))).thenReturn(1);

        ReminderRegisterEvent event = new ReminderRegisterEvent();
        event.setSource("star-site-insurance");
        event.setBizType("policy_payment");
        event.setBizId(7L);
        event.setOwnerMemberId(1L);
        event.setFamilyId(100L);
        event.setTitle("续费");
        event.setFirstRemindAt(LocalDate.now());

        newConsumer(jdbcTemplate).onMessage(envelopeJson("evt-1", "ReminderRegister", event));

        verify(reminderService).upsertRule(any(ReminderRegisterEvent.class));
    }

    @Test
    void duplicateEventSkipped() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), anyString(), eq("evt-dup")))
                .thenThrow(new DuplicateKeyException("uk_group_event"));

        ReminderRegisterEvent event = new ReminderRegisterEvent();
        event.setSource("s");
        event.setBizType("b");
        event.setBizId(1L);

        newConsumer(jdbcTemplate).onMessage(envelopeJson("evt-dup", "ReminderRegister", event));

        verify(reminderService, never()).upsertRule(any(ReminderRegisterEvent.class));
    }

    @Test
    void unknownTypeIgnored() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), anyString(), eq("evt-x"))).thenReturn(1);

        newConsumer(jdbcTemplate).onMessage(envelopeJson("evt-x", "SomeUnknown", new ReminderRegisterEvent()));

        verify(reminderService, never()).upsertRule(any(ReminderRegisterEvent.class));
        verify(reminderService, never()).cancelRule(any());
    }
}
