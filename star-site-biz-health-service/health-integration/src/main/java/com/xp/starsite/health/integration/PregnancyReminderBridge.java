package com.xp.starsite.health.integration;

import com.xp.starsite.api.client.ReminderApi;
import com.xp.starsite.api.dto.ReminderRuleDTO;
import com.xp.starsite.common.rpc.RpcCallGuard;
import com.xp.starsite.common.rpc.RpcIntegration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 提醒中心集成层（SOFA integration 层）：health 域对外部提醒域的唯一出口。
 * 领域内其他类禁止直接依赖提醒域的 Feign。
 * 只负责调用 + 容错（tolerant：提醒是增值能力，失败不阻断主流程）；
 * 产检里程碑规则 DTO 的组装在 health-core-service 的 {@code PrenatalRuleAssembler}。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Component
@RpcIntegration("star-site-platform-reminder")
@RequiredArgsConstructor
public class PregnancyReminderBridge {

    private final ReminderApi reminderApi;

    private final RpcCallGuard rpc;

    /**
     * 批量注册产检里程碑提醒规则（幂等 upsert，单条失败不阻断整批）。
     *
     * @param rules 组装好的规则（见 PrenatalRuleAssembler.milestoneRules）
     * @return 注册成功的规则数
     */
    public int registerMilestoneRules(List<ReminderRuleDTO> rules) {
        int registered = 0;
        for (ReminderRuleDTO rule : rules) {
            if (rpc.attempt("health.reminder.prenatalUpsert", () -> reminderApi.upsertRule(rule))) {
                registered++;
            }
        }
        return registered;
    }
}
