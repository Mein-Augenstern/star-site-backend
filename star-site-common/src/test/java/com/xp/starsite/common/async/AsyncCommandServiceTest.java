package com.xp.starsite.common.async;

import com.xp.starsite.common.async.mapper.AsyncCommandMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** star-async 核心语义单测：退避/死信/幂等提交
 *
 * @author xiaopu
 * @since 2026-10
 */
class AsyncCommandServiceTest {

    private AsyncCommand cmd(long id, int retry, int maxRetry) {
        AsyncCommand c = new AsyncCommand();
        c.setId(id);
        c.setCmdType("T");
        c.setRetryCount(retry);
        c.setMaxRetry(maxRetry);
        return c;
    }

    @Test
    void firstRetryBackoffIs60Seconds() {
        AsyncCommandMapper mapper = mock(AsyncCommandMapper.class);
        AsyncCommandService s = new AsyncCommandService(mapper);
        AsyncCommand c = cmd(1, 0, 5);
        s.markRetryOrFail(c, new RuntimeException("boom"));
        assertEquals(AsyncCommand.PENDING, c.getStatus());
        assertEquals(1, c.getRetryCount());
        long wait = java.time.Duration.between(LocalDateTime.now(), c.getNextRunAt()).getSeconds();
        org.junit.jupiter.api.Assertions.assertTrue(wait >= 55 && wait <= 65, "首次退避应≈60s, got " + wait);
    }

    @Test
    void backoffCappedAtOneHour() {
        AsyncCommandMapper mapper = mock(AsyncCommandMapper.class);
        AsyncCommandService s = new AsyncCommandService(mapper);
        AsyncCommand c = cmd(1, 20, 25);
        s.markRetryOrFail(c, new RuntimeException("x"));
        long wait = java.time.Duration.between(LocalDateTime.now(), c.getNextRunAt()).getSeconds();
        org.junit.jupiter.api.Assertions.assertTrue(wait <= 3610, "退避应封顶 1h, got " + wait);
    }

    @Test
    void exceedMaxRetryGoesDead() {
        AsyncCommandMapper mapper = mock(AsyncCommandMapper.class);
        AsyncCommandService s = new AsyncCommandService(mapper);
        AsyncCommand c = cmd(1, 5, 5);
        s.markRetryOrFail(c, new RuntimeException("final"));
        assertEquals(AsyncCommand.DEAD, c.getStatus());
        assertNotNull(c.getLastError());
    }

    @Test
    void submitIdempotentSkipsWhenPendingExists() {
        AsyncCommandMapper mapper = mock(AsyncCommandMapper.class);
        when(mapper.countUnfinished("AI_INTERPRET", "report:1")).thenReturn(1L);
        AsyncCommandService s = new AsyncCommandService(mapper);
        AsyncCommand r = s.submitIdempotent("AI_INTERPRET", "report:1", "{}");
        assertNull(r);
        verify(mapper, Mockito.never()).insert(any(AsyncCommand.class));
    }

    @Test
    void submitFillsDefaults() {
        AsyncCommandMapper mapper = mock(AsyncCommandMapper.class);
        when(mapper.insert(any(AsyncCommand.class))).thenAnswer(inv -> {
            inv.getArgument(0, AsyncCommand.class).setId(9L);
            return 1;
        });
        AsyncCommandService s = new AsyncCommandService(mapper);
        AsyncCommand r = s.submitAt("T", "k", "{}", LocalDateTime.now().plusMinutes(30));
        assertEquals(9L, r.getId());
        assertEquals(AsyncCommand.PENDING, r.getStatus());
        assertEquals(5, r.getMaxRetry());
    }
}
