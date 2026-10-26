package com.xp.starsite.reminder.task;

import com.xp.starsite.lock.client.LockClient;
import com.xp.starsite.reminder.service.ReminderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 提醒扫描任务的锁门控语义单测：抢到锁才执行 + 主动让位 + 未抢到零副作用。
 * （锁本身的互斥/TTL 语义由组件仓覆盖，这里只验任务的接法。）
 *
 * @author muyi
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class ReminderScanTaskTest {

    @Mock
    private ReminderService reminderService;

    @Mock
    private LockClient lockClient;

    @InjectMocks
    private ReminderScanTask task;

    @Test
    void 抢到锁_执行扫描并主动让位() {
        when(lockClient.tryLock(ReminderScanTask.LOCK_NAME)).thenReturn(true);
        when(reminderService.scanAndNotify()).thenReturn(3);

        task.scan();

        verify(reminderService).scanAndNotify();
        verify(lockClient).unlock(ReminderScanTask.LOCK_NAME);
    }

    @Test
    void 未抢到锁_跳过本轮且不释放他方的锁() {
        when(lockClient.tryLock(ReminderScanTask.LOCK_NAME)).thenReturn(false);

        task.scan();

        verify(reminderService, never()).scanAndNotify();
        // 关键：绝不能 unlock——那是他方实例持有的锁（组件侧 CAS 防误删，任务侧同样不越权）
        verify(lockClient, never()).unlock(anyString());
    }

    @Test
    void 扫描异常_仍然让位() {
        when(lockClient.tryLock(ReminderScanTask.LOCK_NAME)).thenReturn(true);
        when(reminderService.scanAndNotify()).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, task::scan);

        verify(lockClient).unlock(ReminderScanTask.LOCK_NAME);
    }
}
