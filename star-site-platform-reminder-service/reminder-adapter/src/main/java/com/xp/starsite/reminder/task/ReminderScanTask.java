package com.xp.starsite.reminder.task;

import com.xp.starsite.reminder.service.ReminderService;
import com.xp.starsite.schedule.api.MisfirePolicy;
import com.xp.starsite.schedule.api.StarsiteScheduled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 每日提醒扫描：补生成实例 + 投递到期通知。
 * 多实例恰一执行：starsite-schedule 组件经 starsite-lock 争锁门控（锁名 sch:任务名，
 * db 模式）——同一触发时刻全平台只有一个实例真正执行；未获锁实例写 SKIPPED_LOCKED
 * 流水后静默跳过（通知不可回收，重复投递是硬伤）。
 *
 * <p>迁移注记（2026-11-02 组件化试点）：原手写 tryLock/unlock 样板已被
 * {@code @StarsiteScheduled} 吸收——执行流水（RUNNING/DONE/FAILED/SKIPPED_LOCKED/STALE）
 * 落 schedule_run_log 表，错过策略与触发时区（默认 Asia/Shanghai）由组件统一承担。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScanTask {

    /** 任务名（锁名 = sch:reminder-daily-scan，全平台唯一；流水表 task_name 同值） */
    static final String TASK_NAME = "reminder-daily-scan";

    private final ReminderService reminderService;

    /**
     * cron 在 application.yml 配置（默认每天 08:00；注解属性支持 ${} 占位符）。
     * 错过策略 SKIP：通知类任务错过当日就等次日（补发旧通知反而是打扰）。
     */
    @StarsiteScheduled(name = TASK_NAME, cron = "${reminder.scan-cron:0 0 8 * * ?}",
            misfire = MisfirePolicy.SKIP)
    public void scan() {
        int notified = reminderService.scanAndNotify();
        log.info("每日提醒扫描完成: 新投递 {} 条", notified);
    }
}
