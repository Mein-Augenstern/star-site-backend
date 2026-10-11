package com.xp.starsite.health.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.health.ai.ReportInterpretationService;
import com.xp.starsite.health.ai.ReportOcrService;
import com.xp.starsite.health.service.ReportInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报告智能解读接口：LLM 解读（同步/异步）与拍照 OCR 结构化。
 * 拆分约定与鉴权说明见 {@link ReportController} 类注释。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class ReportInsightController {

    private final ReportInsightService reportInsightService;

    /**
     * AI 解读报告（同步，结果落库缓存；force=true 强制重新解读）
     *
     * @param force true 忽略已有解读缓存重新生成
     */
    @PostMapping("/reports/{id}/interpretation")
    public R<ReportInterpretationService.InterpretationResult> interpret(
            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        return R.ok(reportInsightService.interpret(memberId, id, force));
    }

    /**
     * AI 解读异步版：落幂等命令立即返回，后台执行，
     * 结果经报告详情（GET /reports/{id} 的 interpretation 字段）轮询获取。
     * 未配置 LLM_API_KEY 时返回 400。
     */
    @PostMapping("/reports/{id}/interpret-async")
    public R<Boolean> interpretAsync(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                     @PathVariable Long id,
                                     @RequestParam(defaultValue = "false") boolean force) {
        return R.ok(reportInsightService.interpretAsync(memberId, id, force));
    }

    /**
     * 拍照识别报告 → 结构化草稿（预填表单用；人工确认后另行提交落库，本端点不写库）
     */
    @PostMapping("/reports/ocr")
    public R<ReportOcrService.OcrDraft> ocr(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                            @RequestBody OcrRequest req) {
        return R.ok(reportInsightService.ocr(memberId, req.fileId()));
    }

    /**
     * OCR 请求体
     *
     * @param fileId 报告图片的文件 id（file 服务，先经 POST /api/file 上传）
     */
    public record OcrRequest(Long fileId) {
    }
}
