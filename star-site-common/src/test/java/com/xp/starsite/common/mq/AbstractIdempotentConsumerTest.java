package com.xp.starsite.common.mq;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 幂等消费基类单测：验证 (consumerGroup, eventId) 去重语义（mock JdbcTemplate，不连库）——
 * 首次消费执行、重复消息跳过、非重复类 SQL 异常原样上抛交 MQ 重试。
 *
 * @author xiaopu
 * @since 2026-10
 */
class AbstractIdempotentConsumerTest {

    private static final String GROUP = "G_test";

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AbstractIdempotentConsumer consumer = new AbstractIdempotentConsumer(jdbcTemplate, GROUP) {
    };

    @Test
    void firstConsumeExecutes() {
        when(jdbcTemplate.update(anyString(), eq(GROUP), eq("evt-1"))).thenReturn(1);
        AtomicInteger ran = new AtomicInteger();
        boolean first = consumer.consumeIdempotent("evt-1", ran::incrementAndGet);
        assertTrue(first);
        assertEquals(1, ran.get());
    }

    @Test
    void duplicateConsumeSkips() {
        when(jdbcTemplate.update(anyString(), eq(GROUP), eq("evt-dup")))
                .thenThrow(new DuplicateKeyException("uk_group_event"));
        AtomicInteger ran = new AtomicInteger();
        boolean first = consumer.consumeIdempotent("evt-dup", ran::incrementAndGet);
        assertFalse(first);
        assertEquals(0, ran.get());
    }

    @Test
    void unexpectedSqlErrorPropagates() {
        when(jdbcTemplate.update(anyString(), eq(GROUP), eq("evt-err")))
                .thenThrow(new IllegalStateException("db down"));
        assertThrows(IllegalStateException.class,
                () -> consumer.consumeIdempotent("evt-err", () -> { }));
    }
}
