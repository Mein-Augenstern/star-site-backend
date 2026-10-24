package com.xp.starsite.recheck.integration;

import com.xp.starsite.api.client.ReminderApi;
import com.xp.starsite.api.dto.ReminderRuleDTO;
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
 * @author muyi
 * @since 2026-10
 */
@Component
@RpcIntegration("star-site-platform-reminder")
@RequiredArgsConstructor
public class ReminderIntegration {

    private final ReminderApi reminderApi;

    /** 注册规则（tolerant：@RpcTolerant 在 ReminderApi 方法上，切面吞掉失败） */
    public void upsert(ReminderRuleDTO dto) {
        reminderApi.upsertRule(dto);
    }

    /** 撤销规则（tolerant：同上） */
    public void delete(ReminderRuleDTO key) {
        reminderApi.deleteRule(key);
    }
}
