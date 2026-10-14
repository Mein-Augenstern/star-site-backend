package com.xp.starsite.recheck.integration;

import com.xp.starsite.api.client.ReminderApi;
import com.xp.starsite.api.dto.ReminderRuleDTO;
import com.xp.starsite.common.rpc.RpcCallGuard;
import com.xp.starsite.common.rpc.RpcIntegration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 提醒中心集成层（SOFA integration 层）：复查计划对外部提醒域的唯一出口。
 * 领域内其他类禁止直接依赖 reminder 的 Feign。
 * 只负责调用 + 容错（tolerant：提醒是增值能力，失败不阻断主流程）；
 * 规则 DTO 的组装（复查日/每日监测两条规则、幂等键）在
 * recheck-core-service 的 {@code ReminderRuleAssembler}。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RpcIntegration("star-site-platform-reminder")
@RequiredArgsConstructor
public class ReminderIntegration {

    private final ReminderApi reminderApi;

    private final RpcCallGuard rpc;

    /** 注册规则（tolerant：失败吞掉返回 false，error 留痕，不阻断建计划主流程） */
    public boolean upsertTolerant(ReminderRuleDTO dto) {
        return rpc.attempt("recheck.reminder.ruleUpsert", () -> reminderApi.upsertRule(dto));
    }

    /** 撤销规则（tolerant：幂等键定位，失败吞掉不阻断闭环/取消主流程） */
    public boolean deleteTolerant(ReminderRuleDTO key) {
        return rpc.attempt("recheck.reminder.ruleDelete", () -> reminderApi.deleteRule(key));
    }
}
