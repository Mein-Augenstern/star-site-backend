package com.xp.starsite.recheck.integration;

import com.xp.starsite.api.client.HealthApi;
import com.xp.starsite.api.dto.health.HealthReportDTO;
import com.xp.starsite.common.rpc.RpcIntegration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * health 横向服务集成层（SOFA integration 层）：recheck 域读报告/指标的唯一出口。
 * 领域内其他类禁止直接依赖 health 的 Feign——垂直依赖横向只走本层。
 * 与提醒不同，报告读取是复查闭环的强依赖（建计划/闭环都靠它），fail-closed：
 * 失败经 RpcCallGuard 翻译为标准 RpcException（带 retryable）上抛。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RpcIntegration("star-site-biz-health")
@RequiredArgsConstructor
public class HealthIntegration {

    private final HealthApi healthApi;

    /**
     * 报告详情（含全部指标）
     *
     * @throws com.xp.starsite.common.rpc.RpcException health 不可用（fail-closed，retryable=true）
     *         或报告不存在（透传 400 语义，retryable=false）
     */
    public HealthReportDTO getReportDetail(Long memberId, Long reportId) {
        return healthApi.getReportDetail(memberId, reportId).getData();
    }
}
