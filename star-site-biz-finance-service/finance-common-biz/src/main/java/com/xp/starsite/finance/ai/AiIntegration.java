package com.xp.starsite.finance.ai;

import com.xp.starsite.common.rpc.RpcCallGuard;
import com.xp.starsite.common.rpc.RpcIntegration;
import com.xp.starsite.finance.ai.dto.FinanceDiagnosisDTO;
import com.xp.starsite.finance.ai.dto.FinanceDiagnosisRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 智能体服务集成出口（finance 特例：随契约放 common-biz——integration 依赖 common-biz
 * 的 MQ 消费方向不允许反向依赖，见 {@link AiApi} 类注释）。
 * 诊断是实时生成（无缓存语义），fail-closed：失败经 RpcCallGuard 翻译为标准 RpcException 上抛。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RpcIntegration("star-site-platform-ai")
@RequiredArgsConstructor
public class AiIntegration {

    private final AiApi aiApi;

    private final RpcCallGuard rpc;

    /**
     * 财务健康诊断
     *
     * @throws com.xp.starsite.common.rpc.RpcException AI 服务不可用/诊断失败
     */
    public FinanceDiagnosisDTO diagnose(FinanceDiagnosisRequestDTO request) {
        return rpc.call("finance.ai.diagnose", () -> aiApi.financeDiagnosis(request));
    }
}
