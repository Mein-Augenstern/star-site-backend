package com.xp.starsite.health.service;

import com.xp.starsite.health.ai.ReportInterpretationService;
import com.xp.starsite.health.ai.ReportOcrService;

/**
 * 报告智能解读服务：LLM 报告解读（同步/异步）与拍照 OCR 结构化。
 * <p>
 * 异步解读基于 star-async 幂等命令：事务内落命令立即返回，
 * 后台执行，结果经报告详情（GET /reports/{id} 的 interpretation 字段）轮询获取。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface ReportInsightService {

    /**
     * 同步 AI 解读报告（结果落库缓存）
     *
     * @param force true 强制重新解读（忽略已有缓存）
     * @return 解读结果（文本 + 摘要）
     */
    ReportInterpretationService.InterpretationResult interpret(Long memberId, Long reportId, boolean force);

    /**
     * 异步 AI 解读：落幂等命令后立即返回。
     *
     * @return true=命令已受理；未配置 LLM_API_KEY 时抛 400 业务异常
     */
    boolean interpretAsync(Long memberId, Long reportId, boolean force);

    /**
     * 拍照识别报告 → 结构化草稿（预填表单，人工确认后另行提交落库，本方法不写库）
     */
    ReportOcrService.OcrDraft ocr(Long memberId, Long fileId);
}
