package com.xp.starsite.api.client;

import com.xp.starsite.api.dto.ReminderRuleDTO;
import com.xp.starsite.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * reminder 服务内部接口（Feign 契约）：跨服务提醒规则注册/删除。
 * /internal/** 不经网关路由，仅限内网服务间调用。
 */
@FeignClient(name = "star-site-platform-reminder", contextId = "reminderInternalApi", path = "/internal/reminder")
public interface ReminderApi {

    /** upsert 提醒规则（幂等键 source+bizType+bizId） */
    @PostMapping("/rules/upsert")
    R<Void> upsertRule(@RequestBody ReminderRuleDTO dto);

    /** 按幂等键删除规则 */
    @PostMapping("/rules/delete")
    R<Void> deleteRule(@RequestBody ReminderRuleDTO dto);
}
