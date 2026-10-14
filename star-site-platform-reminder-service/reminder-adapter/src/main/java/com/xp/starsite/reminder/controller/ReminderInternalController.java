package com.xp.starsite.reminder.controller;

import com.xp.starsite.api.dto.ReminderRuleDTO;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.reminder.controller.actions.ReminderActions;
import com.xp.starsite.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口（网关无路由，仅内网）。
 * 跨服务提醒规则的注册/撤销（幂等键 source+bizType+bizId）与手动触发扫描；
 * 动作名 reminder.internal.*。调用方统一走 reminder-facade 的 ReminderApi。
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

    /**
     * upsert 提醒规则（幂等键 source+bizType+bizId；重复调用覆盖更新）
     *
     * @param dto 规则内容（source/bizType/bizId 必填）
     */
    @PostMapping("/rules/upsert")
    public R<Void> upsertRule(@RequestBody ReminderRuleDTO dto) {
        return writeTemplate.write(ReminderActions.INTERNAL_RULE_UPSERT,
                () -> reminderService.upsertRuleFromInternal(dto));
    }

    /**
     * 按幂等键删除规则（不存在时静默成功）
     *
     * @param dto 仅 source/bizType/bizId 参与定位
     */
    @PostMapping("/rules/delete")
    public R<Void> deleteRule(@RequestBody ReminderRuleDTO dto) {
        return writeTemplate.write(ReminderActions.INTERNAL_RULE_DELETE,
                () -> reminderService.deleteRuleFromInternal(dto));
    }

    /**
     * 手动触发到期扫描（与定时任务同一逻辑，幂等）
     *
     * @return 本次新投递的通知数
     */
    @PostMapping("/scan")
    public R<Integer> scan() {
        return writeTemplate.write(ReminderActions.INTERNAL_SCAN,
                () -> reminderService.scanAndNotify());
    }
}
