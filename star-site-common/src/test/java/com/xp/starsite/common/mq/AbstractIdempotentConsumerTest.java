package com.xp.starsite.common.mq;

import com.xp.starsite.idempotent.api.IdempotentClient;
import com.xp.starsite.idempotent.clock.TimeSource;
import com.xp.starsite.idempotent.core.IdempotentEngine;
import com.xp.starsite.idempotent.store.memory.MemoryIdempotentStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 幂等消费基类单测：真 starsite-idempotent 引擎 + 内存存储（不连库）——
 * 首次消费执行、窗口内重复跳过、动作失败释放键后重投可重执行（本次迁移修复的
 * 旧缺陷：event_consume 先落行，业务失败后重投被当重复跳过、永不重试）。
 *
 * @author muyi
 * @since 2026-10
 */
class AbstractIdempotentConsumerTest {

    private static final String GROUP = "G_test";

    /** 真引擎 + 内存存储（wait 200ms/轮询 20ms，单测快速收敛） */
    private static AbstractIdempotentConsumer consumer() {
        AtomicInteger seq = new AtomicInteger();
        IdempotentClient client = new IdempotentEngine(
                new MemoryIdempotentStore(seq::incrementAndGet), TimeSource.system(),
                Duration.ofHours(24), Duration.ofMillis(200), 20, Duration.ofSeconds(10));
        return new AbstractIdempotentConsumer(client, GROUP) {
        };
    }

    @Test
    void firstConsumeExecutes() {
        AbstractIdempotentConsumer consumer = consumer();
        AtomicInteger ran = new AtomicInteger();
        boolean first = consumer.consumeIdempotent("evt-1", ran::incrementAndGet);
        assertTrue(first);
        assertEquals(1, ran.get());
    }

    @Test
    void duplicateConsumeSkips() {
        AbstractIdempotentConsumer consumer = consumer();
        AtomicInteger ran = new AtomicInteger();
        assertTrue(consumer.consumeIdempotent("evt-dup", ran::incrementAndGet));
        boolean second = consumer.consumeIdempotent("evt-dup", ran::incrementAndGet);
        assertFalse(second);
        assertEquals(1, ran.get());
    }

    @Test
    void failureReleasesKeyAndRetryReExecutes() {
        AbstractIdempotentConsumer consumer = consumer();
        AtomicInteger ran = new AtomicInteger();
        // 首次执行失败：异常上抛交 MQ 重试（组件标 FAILED 释放键）
        assertThrows(IllegalStateException.class,
                () -> consumer.consumeIdempotent("evt-retry", () -> {
                    ran.incrementAndGet();
                    throw new IllegalStateException("boom");
                }));
        assertEquals(1, ran.get());
        // MQ 重投同 eventId：键已释放 → 重执行成功（旧实现此处被当重复跳过）
        boolean retried = consumer.consumeIdempotent("evt-retry", ran::incrementAndGet);
        assertTrue(retried);
        assertEquals(2, ran.get());
    }

    @Test
    void differentKeysBothExecute() {
        AbstractIdempotentConsumer consumer = consumer();
        AtomicInteger ran = new AtomicInteger();
        assertTrue(consumer.consumeIdempotent("evt-a", ran::incrementAndGet));
        assertTrue(consumer.consumeIdempotent("evt-b", ran::incrementAndGet));
        assertEquals(2, ran.get());
    }
}
