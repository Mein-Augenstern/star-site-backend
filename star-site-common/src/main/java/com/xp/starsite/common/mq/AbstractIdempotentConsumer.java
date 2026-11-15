package com.xp.starsite.common.mq;

import com.xp.starsite.idempotent.api.IdempotentClient;
import com.xp.starsite.idempotent.api.IdempotentResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 幂等消费基类：内核为 starsite-idempotent 组件（{@link IdempotentClient#guard}，
 * scope=消费组、key=eventId；2026-11 由 event_consume 手工表迁移而来，
 * 表由 idempotent_record 接管、event_consume 保留存量数据）。
 * <p>
 * 语义（组件四态映射）：
 * <ul>
 *   <li>EXECUTED：首次消费，已执行 doConsume</li>
 *   <li>REPLAYED：键记忆窗口内重复消息，零执行跳过</li>
 *   <li>doConsume 异常：组件标记 FAILED 后<b>释放键</b>并原样上抛交 MQ 重试，
 *       重投时键已释放可重执行——修复旧实现「先落幂等行再消费，业务失败后
 *       重投被当重复跳过、失败事件永不重试」的缺陷（旧表语义下首败即 at-most-once）</li>
 *   <li>CONFLICT/FAILED（他方在飞等待耗尽/目睹他方失败）：上抛交 MQ 稍后重投</li>
 * </ul>
 * 子类实现 doConsume。注意：组件保证同 eventId 恰一执行，但 doConsume 的业务
 * 落库与幂等记录不在同一事务（各自管理事务），消费端仍须业务可重入（如 upsert）。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
public abstract class AbstractIdempotentConsumer {

    private final IdempotentClient idempotentClient;
    private final String consumerGroup;

    protected AbstractIdempotentConsumer(IdempotentClient idempotentClient, String consumerGroup) {
        this.idempotentClient = idempotentClient;
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
     * doConsume 异常与未获终态（CONFLICT/FAILED）原样上抛交 MQ 重试。
     *
     * @param traceId 发送方透传的 traceId（消息属性 trace_id，可空）
     */
    protected final boolean consumeIdempotent(String topic, String tag, String eventId,
                                              String traceId, Runnable doConsume) {
        com.xp.starsite.common.log.TraceContext.set(traceId);
        long start = System.currentTimeMillis();
        try {
            IdempotentResult result;
            try {
                result = idempotentClient.guard(consumerGroup, eventId, () -> {
                    doConsume.run();
                    return "ok";
                });
            } catch (Exception e) {
                // 动作失败：组件已标 FAILED 释放键后重抛——重投可重执行
                emitConsumeDigest(topic, tag, eventId, "error", e.getClass().getSimpleName(), start);
                throw e;
            }
            if (result.isExecuted()) {
                emitConsumeDigest(topic, tag, eventId, "success", null, start);
                return true;
            }
            if (result.isReplayed()) {
                log.info("重复事件，跳过: group={}, eventId={}", consumerGroup, eventId);
                emitConsumeDigest(topic, tag, eventId, "duplicate", null, start);
                return false;
            }
            // CONFLICT（他方在飞且等待窗口耗尽）/ FAILED（目睹他方失败）：无确定性结局，
            // 上抛交 MQ 稍后重投（勿 ACK——首执行可能仍在飞或已失败）
            String err = result.getError() != null ? result.getError() : result.getKind().name();
            emitConsumeDigest(topic, tag, eventId, "error", err, start);
            throw new IllegalStateException("幂等消费未获终态，交 MQ 重投: group=" + consumerGroup
                    + ", eventId=" + eventId + ", kind=" + result.getKind());
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
