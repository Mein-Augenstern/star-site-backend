package com.xp.starsite.reminder.controller;

import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口（网关无路由，仅内网）。
 * 调试/运维手动触发扫描（与定时任务同一逻辑，幂等）；动作名 reminder.internal.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/internal/reminder")
@RequiredArgsConstructor
public class ReminderInternalController {

    private final ReminderService reminderService;
    private final WriteTemplate writeTemplate;

    /** 手动触发到期扫描（返回本次生成的提醒实例数） */
    @PostMapping("/scan")
    public R<Integer> scan() {
        return R.ok(writeTemplate.execute("reminder.internal.scan",
                () -> reminderService.scanAndNotify()));
    }
}
