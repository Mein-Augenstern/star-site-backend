package com.xp.starsite.finance.ai;

import com.xp.starsite.common.result.R;
import com.xp.starsite.finance.ai.dto.FinanceDiagnosisDTO;
import com.xp.starsite.finance.ai.dto.FinanceDiagnosisRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI 智能体服务内部接口（Feign 契约，跨语言：提供方为 Python/FastAPI + LangChain）。
 * AI 服务是平台唯一的 LLM 出口（财务顾问 prompt/模型在对方），finance 只发结构化快照。
 * 契约归消费方模块的例外：finance-integration 依赖 common-biz（MQ 消费方向），
 * common-biz 不能反向依赖，故跨语言契约放本模块（docs/技术/12 §跨语言规约）。
 *
 * /internal/** 不经网关路由（外网 404），仅限内网服务间调用。
 *
 * @author xiaopu
 * @since 2026-10
 */
@FeignClient(name = "star-site-platform-ai", contextId = "financeAiInternalApi", path = "/internal/ai")
public interface AiApi {

    /**
     * 财务健康诊断：三张表 + 收支快照 → 诊断建议文本
     */
    @PostMapping("/finance-diagnosis")
    R<FinanceDiagnosisDTO> financeDiagnosis(@RequestBody FinanceDiagnosisRequestDTO request);
}
