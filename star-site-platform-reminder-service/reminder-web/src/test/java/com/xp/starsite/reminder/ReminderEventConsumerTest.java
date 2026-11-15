package com.xp.starsite.reminder;

import com.xp.starsite.api.event.reminder.ReminderRegisterEvent;
import com.xp.starsite.idempotent.api.IdempotentClient;
import com.xp.starsite.idempotent.clock.TimeSource;
import com.xp.starsite.idempotent.core.IdempotentEngine;
import com.xp.starsite.idempotent.store.memory.MemoryIdempotentStore;
import com.xp.starsite.reminder.mq.ReminderEventConsumer;
import com.xp.starsite.reminder.service.ReminderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xp.starsite.common.mq.event.EventEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 提醒事件消费者测试：信封解码 + eventId 幂等分发（真 starsite-idempotent 引擎 + 内存存储）
 *
 * @author muyi
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class ReminderEventConsumerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private ReminderService reminderService;

    private ReminderEventConsumer newConsumer() {
        AtomicInteger seq = new AtomicInteger();
        IdempotentClient client = new IdempotentEngine(
                new MemoryIdempotentStore(seq::incrementAndGet), TimeSource.system(),
                Duration.ofHours(24), Duration.ofMillis(200), 20, Duration.ofSeconds(10));
        return new ReminderEventConsumer(client, reminderService);
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
        ReminderRegisterEvent event = new ReminderRegisterEvent();
        event.setSource("star-site-insurance");
        event.setBizType("policy_payment");
        event.setBizId(7L);
        event.setOwnerMemberId(1L);
        event.setFamilyId(100L);
        event.setTitle("续费");
        event.setFirstRemindAt(LocalDate.now());

        newConsumer().onMessage(envelopeJson("evt-1", "ReminderRegister", event));

        verify(reminderService).upsertRule(any(ReminderRegisterEvent.class));
    }

    @Test
    void duplicateEventSkipped() throws Exception {
        ReminderRegisterEvent event = new ReminderRegisterEvent();
        event.setSource("s");
        event.setBizType("b");
        event.setBizId(1L);
        String raw = envelopeJson("evt-dup", "ReminderRegister", event);

        ReminderEventConsumer consumer = newConsumer();
        consumer.onMessage(raw);
        consumer.onMessage(raw);

        verify(reminderService, times(1)).upsertRule(any(ReminderRegisterEvent.class));
    }

    @Test
    void unknownTypeIgnored() throws Exception {
        newConsumer().onMessage(envelopeJson("evt-x", "SomeUnknown", new ReminderRegisterEvent()));

        verify(reminderService, never()).upsertRule(any(ReminderRegisterEvent.class));
        verify(reminderService, never()).cancelRule(any());
    }
}
