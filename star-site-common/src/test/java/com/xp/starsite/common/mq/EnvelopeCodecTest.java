package com.xp.starsite.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xp.starsite.common.mq.event.EventEnvelope;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 事件信封编解码单测：验证 JSON 往返保留 LocalDateTime（发布端与消费端共用 jsr310 配置的约定），
 * 坏报文解码抛异常进 DLQ。
 *
 * @author xiaopu
 * @since 2026-10
 */
class EnvelopeCodecTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void envelopeRoundTripKeepsLocalDateTime() throws Exception {
        EventEnvelope src = EventEnvelope.builder()
                .eventId("uuid-1")
                .eventType("ReminderRegister")
                .sourceService("star-site-insurance")
                .bizType("policy_payment")
                .bizId(42L)
                .occurredAt(LocalDateTime.of(2026, 10, 6, 20, 30, 0))
                .payloadJson("{\"title\":\"续保提醒\"}")
                .build();

        EventEnvelope decoded = EnvelopeDecoder.decode(MAPPER.writeValueAsString(src));
        assertEquals(src.getEventId(), decoded.getEventId());
        assertEquals(src.getEventType(), decoded.getEventType());
        assertEquals(src.getBizId(), decoded.getBizId());
        assertEquals(src.getOccurredAt(), decoded.getOccurredAt());
        assertEquals(src.getPayloadJson(), decoded.getPayloadJson());
    }

    @Test
    void brokenPayloadThrowsForDlq() {
        assertThrows(IllegalStateException.class, () -> EnvelopeDecoder.decode("not-json"));
    }
}
