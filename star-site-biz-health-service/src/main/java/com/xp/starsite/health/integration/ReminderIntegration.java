package com.xp.starsite.health.integration;

import com.xp.starsite.api.client.ReminderApi;
import com.xp.starsite.api.dto.ReminderRuleDTO;
import com.xp.starsite.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 提醒中心集成层（SOFA integration 层）：health 域对外部提醒域的唯一出口。
 * 领域内其他类禁止直接依赖提醒域的 Feign/Mapper。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderIntegration {

    private final ReminderApi reminderApi;

    /** 产检/孕期关键节点批量注册提醒（幂等，upsert 语义） */
    public void registerMilestones(Long familyMemberId, List<ReminderRuleDTO> rules) {
        for (ReminderRuleDTO rule : rules) {
            try {
                reminderApi.upsertRule(rule);
            } catch (Exception e) {
                // 单条失败不阻断整批（提醒是增值能力，非强一致）
                log.error("产检提醒注册失败: familyMemberId={}, title={}", familyMemberId,
                        rule.getTitle(), e);
            }
        }
    }
}
