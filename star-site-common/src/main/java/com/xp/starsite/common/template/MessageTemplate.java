package com.xp.starsite.common.template;

import com.xp.starsite.common.log.DigestLoggers;
import com.xp.starsite.common.log.TraceContext;

import java.util.function.Supplier;

/**
 * 消息消费模板：MQ 消费端（rocketmq listener）统一包裹的模板变体。
 * <p>
 * 职责：① 从消息属性还原 traceId（发送方埋的，消费日志与发送日志同链路可查）；
 * ② mq-consume-digest 留痕（topic/tag/eventId/状态/耗时）；③ 异常原样上抛交 MQ 重试
 * （配合 AbstractIdempotentConsumer 的幂等去重，重试安全）。
 *
 * @author xiaopu
 * @since 2026-10
 */
public class MessageTemplate {

    /**
     * 包裹一次消息消费。
     *
     * @param topic      消息 topic
     * @param tag        事件类型（tag）
     * @param eventId    事件唯一 id（幂等键，日志串联用）
     * @param traceId    发送方透传的 traceId（可空）
     * @param consumption 消费动作
     * @return 消费动作的返回值（透传）
     */
    public <T> T consume(String topic, String tag, String eventId, String traceId, Supplier<T> consumption) {
        TraceContext.set(traceId);
        long start = System.currentTimeMillis();
        try {
            T result = consumption.get();
            DigestLoggers.emit(DigestLoggers.MQ_CONSUME, DigestLoggers.fields(
                    "topic", topic, "tag", tag, "eventId", eventId,
                    "status", "success", "costMs", System.currentTimeMillis() - start));
            return result;
        } catch (Exception e) {
            DigestLoggers.emit(DigestLoggers.MQ_CONSUME, DigestLoggers.fields(
                    "topic", topic, "tag", tag, "eventId", eventId,
                    "status", "error", "error", e.getClass().getSimpleName(),
                    "costMs", System.currentTimeMillis() - start));
            // 原样上抛：交 MQ 重试（消费端须自行保证业务可重入）
            throw e;
        } finally {
            TraceContext.clear();
        }
    }

    /** 无返回值版本 */
    public void consume(String topic, String tag, String eventId, String traceId, Runnable consumption) {
        consume(topic, tag, eventId, traceId, () -> {
            consumption.run();
            return null;
        });
    }

    /**
     * 消息发送留痕（供 EventPublisher 调用）：topic/tag/eventId/状态/耗时。
     */
    public static void sendDigest(String topic, String tag, String eventId, boolean transactional,
                                  boolean success, long costMs, String error) {
        DigestLoggers.emit(DigestLoggers.MQ_SEND, DigestLoggers.fields(
                "topic", topic, "tag", tag, "eventId", eventId,
                "tx", transactional, "status", success ? "success" : "error",
                "error", error, "costMs", costMs));
    }

    /**
     * 消息消费留痕（供 AbstractIdempotentConsumer 调用）。
     */
    public static void consumeDigest(String topic, String tag, String eventId, String consumerGroup,
                                     String status, String error, long costMs) {
        DigestLoggers.emit(DigestLoggers.MQ_CONSUME, DigestLoggers.fields(
                "topic", topic, "tag", tag, "eventId", eventId, "group", consumerGroup,
                "status", status, "error", error, "costMs", costMs));
    }
}
