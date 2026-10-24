package com.xp.starsite.reminder.mq;

import com.xp.starsite.api.event.reminder.ReminderCancelEvent;
import com.xp.starsite.api.event.reminder.ReminderRegisterEvent;
import com.xp.starsite.common.mq.AbstractIdempotentConsumer;
import com.xp.starsite.common.mq.EnvelopeDecoder;
import com.xp.starsite.common.mq.event.EventEnvelope;
import com.xp.starsite.common.mq.event.EventTypes;
import com.xp.starsite.reminder.service.ReminderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 提醒事件消费者：Register/Cancel 经 eventId 幂等后分发到规则 upsert/撤销。
 * 消费失败 MQ 自动重试，超限进 DLQ。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@RocketMQMessageListener(
        topic = "TOPIC_REMINDER",
        consumerGroup = "G_reminder_reminder",
        consumeThreadNumber = 2)
public class ReminderEventConsumer extends AbstractIdempotentConsumer implements RocketMQListener<String> {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ReminderService reminderService;

    public ReminderEventConsumer(JdbcTemplate jdbcTemplate, ReminderService reminderService) {
        super(jdbcTemplate, "G_reminder_reminder");
        this.reminderService = reminderService;
    }

    @Override
    public void onMessage(String raw) {
        EventEnvelope env = EnvelopeDecoder.decode(raw);
        consumeIdempotent("TOPIC_REMINDER", env.getEventType(), env.getEventId(),
                env.getTraceId(), () -> dispatch(env));
    }

    private void dispatch(EventEnvelope env) {
        switch (env.getEventType()) {
            case EventTypes.REMINDER_REGISTER -> reminderService.upsertRule(readPayload(env, ReminderRegisterEvent.class));
            case EventTypes.REMINDER_CANCEL -> reminderService.cancelRule(readPayload(env, ReminderCancelEvent.class));
            default -> log.warn("未知事件类型，忽略: type={}", env.getEventType());
        }
    }

    private <T> T readPayload(EventEnvelope env, Class<T> type) {
        try {
            return MAPPER.readValue(env.getPayloadJson(), type);
        } catch (Exception e) {
            throw new IllegalStateException("事件载荷反序列化失败: " + env.getEventType(), e);
        }
    }
}
