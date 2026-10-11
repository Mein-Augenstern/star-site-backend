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
        try {
            jdbcTemplate.update(
                    "INSERT INTO event_consume (consumer_group, event_id, consumed_at) VALUES (?, ?, NOW())",
                    consumerGroup, eventId);
        } catch (DuplicateKeyException e) {
            log.info("重复事件，跳过: group={}, eventId={}", consumerGroup, eventId);
            return false;
        }
        doConsume.run();
        return true;
    }

    /** 当前消费组名（日志与排查用） */
    protected final String consumerGroup() {
        return consumerGroup;
    }
}
