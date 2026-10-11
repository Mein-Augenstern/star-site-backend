package com.xp.starsite.reminder.task;

import com.xp.starsite.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日提醒扫描：补生成实例 + 投递到期通知。
 * 单实例部署无需分布式锁；多实例部署时再引入（技术雷达后置项）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScanTask {

    private final ReminderService reminderService;

    @Value("${reminder.scan-cron:0 0 8 * * ?}")
    private String cronHint;

    /** cron 在 application.yml 配置（默认每天 08:00） */
    @Scheduled(cron = "${reminder.scan-cron:0 0 8 * * ?}")
    public void scan() {
        int notified = reminderService.scanAndNotify();
        log.info("每日提醒扫描完成: cron={}, 新投递 {} 条", cronHint, notified);
    }
}
