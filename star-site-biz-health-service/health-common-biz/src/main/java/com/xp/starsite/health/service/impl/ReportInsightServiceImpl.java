package com.xp.starsite.health.service.impl;

import com.xp.starsite.asynccmd.client.AsyncCommandClient;
import com.xp.starsite.health.ai.AiInterpretExecutor;
import com.xp.starsite.health.ai.ReportInterpretationService;
import com.xp.starsite.health.ai.ReportOcrService;
import com.xp.starsite.health.service.ReportInsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * 报告智能解读服务实现：同步/异步解读与 OCR 的编排壳。
 * <p>
 * 异步命令的 LLM key 前置校验放在这里（而非 Controller）：
 * llm.api-key 为空白时 LLM Bean 会因条件注解误创建，以配置值为准，
 * 提交必死命令不如入口直接拒绝。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportInsightServiceImpl implements ReportInsightService {

    /** 异步解读命令未配置 LLM 时的业务错误码 */
    private static final int ERR_LLM_NOT_CONFIGURED = 400;

    private final ReportInterpretationService interpretationService;
    private final ReportOcrService ocrService;
    private final AsyncCommandClient asyncCommandClient;
    private final Environment env;

    @Override
    public ReportInterpretationService.InterpretationResult interpret(Long memberId, Long reportId, boolean force) {
        return interpretationService.interpret(memberId, reportId, force);
    }

    @Override
    public boolean interpretAsync(Long memberId, Long reportId, boolean force) {
        if (env.getProperty("llm.api-key", "").isBlank()) {
            throw new com.xp.starsite.common.exception.BizException(
                    ERR_LLM_NOT_CONFIGURED, "AI 解读未启用（未配置 LLM_API_KEY）");
        }
        String payload = "{\"memberId\":" + memberId + ",\"reportId\":" + reportId + ",\"force\":" + force + "}";
        asyncCommandClient.submitIdempotent(AiInterpretExecutor.TYPE, "report:" + reportId, payload);
        return true;
    }

    @Override
    public ReportOcrService.OcrDraft ocr(Long memberId, Long fileId) {
        return ocrService.ocr(fileId);
    }
}
