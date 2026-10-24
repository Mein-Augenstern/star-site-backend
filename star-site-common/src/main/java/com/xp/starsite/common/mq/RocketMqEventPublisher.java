package com.xp.starsite.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xp.starsite.common.mq.event.EventEnvelope;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RocketMQ 事件发布器实现：自建 DefaultMQProducer + 内置 TransactionListener。
 * <p>
 * 不用 rocketmq-spring 的自动装配 Producer，因为 common 模块不能有 @Component
 * （启动类 scanBasePackages 扫全 starsite 包，裸组件会污染网关）；
 * 由 RocketMqConfig 显式 @Bean 装配、随容器销毁。
 *
 * @author muyi
 * @since 2026-10
 */
class RocketMqEventPublisher implements EventPublisher, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RocketMqEventPublisher.class);

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    /** 单事件体上限（RocketMQ 默认 4MB，业务事件按远小于此假设，可经 star.mq.max-body-bytes 调整） */
    private final int maxBodyBytes;

    private final String sourceService;
    private final TransactionMQProducer producer;

    RocketMqEventPublisher(String nameServer, String producerGroup, String sourceService,
                           int sendMsgTimeoutMs, int maxBodyBytes) {
        this.sourceService = sourceService;
        this.maxBodyBytes = maxBodyBytes;
        // 事务消息须用 TransactionMQProducer（listener 只能在构造时传入）
        this.producer = new TransactionMQProducer(producerGroup);
        this.producer.setNamesrvAddr(nameServer);
        this.producer.setSendMsgTimeout(sendMsgTimeoutMs);
        this.producer.setInstanceName("event-publisher-" + UUID.randomUUID());
        this.producer.setTransactionListener(new DomainTransactionListener());
        try {
            this.producer.start();
        } catch (Exception e) {
            throw new IllegalStateException("RocketMQ producer 启动失败: " + e.getMessage(), e);
        }
        log.info("EventPublisher 就绪: namesrv={}, group={}", nameServer, producerGroup);
    }

    @Override
    public void publish(String topic, String eventType, String bizType, Long bizId, Object payload) {
        EventEnvelope env = envelope(eventType, bizType, bizId, payload);
        Message msg = toMessage(topic, env);
        long start = System.currentTimeMillis();
        try {
            SendResult r = producer.send(msg);
            if (r.getSendStatus() != SendStatus.SEND_OK) {
                throw new IllegalStateException("消息发送失败: " + r.getSendStatus());
            }
            com.xp.starsite.common.template.MessageTemplate.sendDigest(
                    topic, eventType, env.getEventId(), false, true,
                    System.currentTimeMillis() - start, null);
            log.debug("事件已发送: topic={}, type={}, eventId={}", topic, eventType, env.getEventId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendDigestFail(topic, eventType, env.getEventId(), false, start, "interrupted");
            throw new IllegalStateException("消息发送被中断", e);
        } catch (Exception e) {
            sendDigestFail(topic, eventType, env.getEventId(), false, start, e.getMessage());
            throw new IllegalStateException("消息发送异常: " + e.getMessage(), e);
        }
    }

    @Override
    public void publishTransactional(String topic, String eventType, String bizType, Long bizId, Object payload,
                                     LocalTransactionExecutor localTransaction) {
        EventEnvelope env = envelope(eventType, bizType, bizId, payload);
        Message msg = toMessage(topic, env);
        long start = System.currentTimeMillis();
        try {
            SendResult r = producer.sendMessageInTransaction(msg, new PendingTx(env.getEventId(), localTransaction));
            if (r == null || r.getSendStatus() != SendStatus.SEND_OK) {
                // 半消息发送失败：本地事务还没执行，这里抛异常即可让调用方回滚业务
                throw new IllegalStateException("事务半消息发送失败: " + (r == null ? "null" : r.getSendStatus()));
            }
            com.xp.starsite.common.template.MessageTemplate.sendDigest(
                    topic, eventType, env.getEventId(), true, true,
                    System.currentTimeMillis() - start, null);
        } catch (org.apache.rocketmq.client.exception.MQClientException e) {
            sendDigestFail(topic, eventType, env.getEventId(), true, start, e.getMessage());
            throw new IllegalStateException("事务消息发送异常: " + e.getMessage(), e);
        }
    }

    /** 发送失败也留痕（排障时 mq-send-digest 有失败记录可查） */
    private void sendDigestFail(String topic, String eventType, String eventId,
                                boolean transactional, long start, String error) {
        com.xp.starsite.common.template.MessageTemplate.sendDigest(
                topic, eventType, eventId, transactional, false,
                System.currentTimeMillis() - start, error);
    }

    private EventEnvelope envelope(String eventType, String bizType, Long bizId, Object payload) {
        try {
            String payloadJson = payload == null ? null : MAPPER.writeValueAsString(payload);
            return EventEnvelope.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .sourceService(sourceService)
                    .bizType(bizType)
                    .bizId(bizId)
                    .occurredAt(LocalDateTime.now())
                    .payloadJson(payloadJson)
                    .traceId(com.xp.starsite.common.log.TraceContext.current())
                    .build();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("事件序列化失败", e);
        }
    }

    private Message toMessage(String topic, EventEnvelope env) {
        try {
            byte[] body = MAPPER.writeValueAsBytes(env);
            if (body.length > maxBodyBytes) {
                throw new IllegalStateException("事件体超过 " + maxBodyBytes + " 字节，拒绝发送: eventId=" + env.getEventId());
            }
            Message msg = new Message(topic, env.getEventType(), body);
            msg.setKeys(env.getBizId() == null ? env.getEventId() : String.valueOf(env.getBizId()));
            msg.putUserProperty("eventId", env.getEventId());
            // 链路透传：消费端还原 traceId，发送/消费日志同链路可查
            msg.putUserProperty(com.xp.starsite.common.log.TraceContext.MQ_KEY_TRACE_ID,
                    com.xp.starsite.common.log.TraceContext.current());
            return msg;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("事件序列化失败", e);
        }
    }

    @Override
    public void destroy() {
        producer.shutdown();
    }

    /** 事务回调上下文：DomainTransactionListener 从这里取本地事务 */
    record PendingTx(String eventId, LocalTransactionExecutor executor) {
    }
}
