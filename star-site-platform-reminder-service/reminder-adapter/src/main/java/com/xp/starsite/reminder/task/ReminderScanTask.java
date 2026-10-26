package com.xp.starsite.reminder.task;

import com.xp.starsite.lock.client.LockClient;
import com.xp.starsite.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日提醒扫描：补生成实例 + 投递到期通知。
 * 多实例防重跑：经 starsite-lock 组件抢锁门控（db 模式，锁行主键由 sequence 组件发号）——
 * 同一时刻全平台只有一个实例真正执行；未抢到锁的实例本轮直接跳过（通知不可回收，重复投递是硬伤）。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScanTask {

    /** 调度锁名（component_schedule_lock 表的 lock_name，全平台唯一即可） */
    static final String LOCK_NAME = "reminder-daily-scan";

    private final ReminderService reminderService;
    private final LockClient lockClient;

    @Value("${reminder.scan-cron:0 0 8 * * ?}")
    private String cronHint;

    /** cron 在 application.yml 配置（默认每天 08:00） */
    @Scheduled(cron = "${reminder.scan-cron:0 0 8 * * ?}")
    public void scan() {
        if (!lockClient.tryLock(LOCK_NAME)) {
            // 他方实例持锁执行中：跳过本轮（TTL 兜底——持锁者崩溃后锁自动过期，次日不受影响）
            log.info("每日提醒扫描跳过：他方实例持锁执行中");
            return;
        }
        try {
            int notified = reminderService.scanAndNotify();
            log.info("每日提醒扫描完成: cron={}, 新投递 {} 条", cronHint, notified);
        } finally {
            // 主动释放让位（不等 TTL 过期）；释放异常组件内部已吞掉并等 TTL 自动过期
            lockClient.unlock(LOCK_NAME);
        }
    }
}
