package com.xp.starsite.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xp.starsite.common.mq.event.EventEnvelope;

/**
 * 信封化解码工具：@RocketMQMessageListener 的 payload 是裸 String，
 * 先 decode 成 EventEnvelope 再按 eventType 分发处理。
 *
 * @author muyi
 * @since 2026-10
 */
public final class EnvelopeDecoder {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private EnvelopeDecoder() {
    }

    public static EventEnvelope decode(String raw) {
        try {
            return MAPPER.readValue(raw, EventEnvelope.class);
        } catch (Exception e) {
            // 解码失败的消息重试也不会成功，直接抛出让其进 DLQ 排查
            throw new IllegalStateException("事件信封解码失败", e);
        }
    }
}
