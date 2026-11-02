package com.xp.starsite.reminder.task;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xp.starsite.reminder.service.ReminderService;
import com.xp.starsite.schedule.api.MisfirePolicy;
import com.xp.starsite.schedule.api.StarsiteScheduled;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 提醒扫描任务的接法单测（2026-11-02 组件化迁移后）：
 * 锁门控/恰一/流水语义由 starsite-schedule 组件仓覆盖（55 项测试），
 * 这里只验任务侧三件事——业务调用、异常向上交组件吞噬留痕、注解元数据钉死。
 *
 * @author muyi
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class ReminderScanTaskTest {

    @Mock
    private ReminderService reminderService;

    @InjectMocks
    private ReminderScanTask task;

    @Test
    void 扫描调用领域服务() {
        when(reminderService.scanAndNotify()).thenReturn(3);

        task.scan();

        verify(reminderService).scanAndNotify();
    }

    @Test
    void 领域异常向上抛_由组件吞噬并落FAILED流水() {
        // 异常不吞在任务体里：ExactlyOnceRunner 接住落 FAILED + error 摘要
        when(reminderService.scanAndNotify()).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, task::scan);
    }

    @Test
    void 注解元数据_恰一语义与错过策略钉死() throws Exception {
        Method scan = ReminderScanTask.class.getMethod("scan");
        StarsiteScheduled anno = scan.getAnnotation(StarsiteScheduled.class);

        org.junit.jupiter.api.Assertions.assertEquals(ReminderScanTask.TASK_NAME, anno.name());
        org.junit.jupiter.api.Assertions.assertEquals("${reminder.scan-cron:0 0 8 * * ?}", anno.cron());
        org.junit.jupiter.api.Assertions.assertTrue(anno.guard(), "恰一执行不可退化为本地模式");
        org.junit.jupiter.api.Assertions.assertEquals(MisfirePolicy.SKIP, anno.misfire(),
                "通知类任务错过等次日——补发旧通知是打扰");
        org.junit.jupiter.api.Assertions.assertEquals("Asia/Shanghai", anno.zone(),
                "触发时区钉死北京（容器 UTC 陷阱实证）");
    }
}
