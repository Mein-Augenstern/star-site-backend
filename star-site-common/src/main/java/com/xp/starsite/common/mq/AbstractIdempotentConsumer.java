package com.xp.starsite.common.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 幂等消费基类：以 (consumerGroup, eventId) 唯一键落 event_consume 表去重。
 * <p>
 * 子类实现 doConsume；重复消息直接跳过。注意：doConsume 与幂等记录
 * 不在同一事务（消费端各自管理业务事务），因此本基类适用于
 * 「至少一次投递 + 业务幂等」的场景——doConsume 内部仍须保证业务可重入
 * （如 upsert），极端情况（业务成功、记录幂等失败）由 MQ 重试兜底。
 */
@Slf4j
public abstract class AbstractIdempotentConsumer {

    private final JdbcTemplate jdbcTemplate;
    private final String consumerGroup;

    protected AbstractIdempotentConsumer(JdbcTemplate jdbcTemplate, String consumerGroup) {
        this.jdbcTemplate = jdbcTemplate;
        this.consumerGroup = consumerGroup;
    }

    /**
     * @return true = 首次消费，已执行 doConsume；false = 重复消息，已跳过
     */
    protected final boolean consumeIdempotent(String eventId, Runnable doConsume) {
        return consumeIdempotent("unknown", "unknown", eventId, null, doConsume);
    }

    /**
     * 带留痕的幂等消费：mq-consume-digest 一行/次（topic/tag/eventId/状态/耗时），
     * doConsume 异常原样上抛交 MQ 重试（业务可重入由子类保证）。
     *
     * @param traceId 发送方透传的 traceId（消息属性 trace_id，可空）
     */
    protected final boolean consumeIdempotent(String topic, String tag, String eventId,
                                              String traceId, Runnable doConsume) {
        com.xp.starsite.common.log.TraceContext.set(traceId);
        long start = System.currentTimeMillis();
        boolean first;
        try {
            jdbcTemplate.update(
                    "INSERT INTO event_consume (consumer_group, event_id, consumed_at) VALUES (?, ?, NOW())",
                    consumerGroup, eventId);
            first = true;
        } catch (DuplicateKeyException e) {
            log.info("重复事件，跳过: group={}, eventId={}", consumerGroup, eventId);
            emitConsumeDigest(topic, tag, eventId, "duplicate", null, start);
            return false;
        }
        try {
            doConsume.run();
            emitConsumeDigest(topic, tag, eventId, "success", null, start);
            return true;
        } catch (Exception e) {
            emitConsumeDigest(topic, tag, eventId, "error", e.getClass().getSimpleName(), start);
            throw e;
        } finally {
            com.xp.starsite.common.log.TraceContext.clear();
        }
    }

    private void emitConsumeDigest(String topic, String tag, String eventId,
                                   String status, String error, long start) {
        com.xp.starsite.common.template.MessageTemplate.consumeDigest(
                topic, tag, eventId, consumerGroup, status, error, System.currentTimeMillis() - start);
    }

    /** 当前消费组名（日志与排查用） */
    protected final String consumerGroup() {
        return consumerGroup;
    }
}
