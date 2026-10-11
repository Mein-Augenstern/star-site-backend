package com.xp.starsite.health.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.health.dto.IndicatorBatchDTO;
import com.xp.starsite.health.dto.MetricBatchDTO;
import com.xp.starsite.health.dto.PregnancyUpsertDTO;
import com.xp.starsite.health.dto.ReportCreateDTO;
import com.xp.starsite.health.dto.VisitCreateDTO;
import com.xp.starsite.health.service.HealthService;
import com.xp.starsite.health.vo.HealthReportVO;
import com.xp.starsite.health.vo.MetricVO;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.PregnancyVO;
import com.xp.starsite.health.vo.TrendPointVO;
import com.xp.starsite.health.vo.VisitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 体检报告接口（全部需 JWT；memberId 用于归属校验，防横向越权）
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;
    private final com.xp.starsite.health.ai.ReportInterpretationService interpretationService;
    private final com.xp.starsite.health.ai.ReportOcrService ocrService;
    private final com.xp.starsite.common.async.AsyncCommandService asyncCommandService;
    private final org.springframework.core.env.Environment env;

    @PostMapping("/reports")
    public R<HealthReportVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                    @Valid @RequestBody ReportCreateDTO dto) {
        return R.ok(healthService.createReport(memberId, dto));
    }

    @GetMapping("/reports")
    public R<PageVO<HealthReportVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                          @RequestParam(required = false) Long familyMemberId,
                                          @RequestParam(required = false) Integer year,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return R.ok(healthService.listReports(memberId, familyMemberId, year, page, size));
    }

    @GetMapping("/reports/{id}")
    public R<HealthReportVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                    @PathVariable Long id) {
        return R.ok(healthService.getDetail(memberId, id));
    }

    @PostMapping("/reports/{id}/indicators")
    public R<Void> addIndicators(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                 @PathVariable Long id,
                                 @Valid @RequestBody IndicatorBatchDTO dto) {
        healthService.addIndicators(memberId, id, dto);
        return R.ok();
    }

    /** AI 解读报告（结果落库缓存；force=true 强制重新解读） */
    @PostMapping("/reports/{id}/interpretation")
    public R<com.xp.starsite.health.ai.ReportInterpretationService.InterpretationResult> interpret(
            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        return R.ok(interpretationService.interpret(memberId, id, force));
    }

    /** AI 解读异步版：事务内落命令立即返回，后台执行，结果经 GET /reports/{id} 轮询获取 */
    @PostMapping("/reports/{id}/interpret-async")
    public R<Boolean> interpretAsync(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                     @PathVariable Long id,
                                     @RequestParam(defaultValue = "false") boolean force) {
        // LLM key 空白=未配置（bean 会因条件注解误创建，这里以配置值为准），提交必死命令提前拒绝
        if (env.getProperty("llm.api-key", "").isBlank()) {
            return R.fail(400, "AI 解读未启用（未配置 LLM_API_KEY）");
        }
        String payload = "{\"memberId\":" + memberId + ",\"reportId\":" + id + ",\"force\":" + force + "}";
        asyncCommandService.submitIdempotent(
                com.xp.starsite.health.ai.AiInterpretExecutor.TYPE, "report:" + id, payload);
        return R.ok(true);
    }

    /** 拍照识别报告 → 结构化草稿（预填表单，人工确认后另行提交落库） */
    @PostMapping("/reports/ocr")
    public R<com.xp.starsite.health.ai.ReportOcrService.OcrDraft> ocr(
            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
            @RequestBody OcrRequest req) {
        return R.ok(ocrService.ocr(req.fileId()));
    }

    public record OcrRequest(Long fileId) {
    }

    @DeleteMapping("/reports/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        healthService.deleteReport(memberId, id);
        return R.ok();
    }

    @GetMapping("/trends")
    public R<List<TrendPointVO>> trends(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                        @RequestParam Long familyMemberId,
                                        @RequestParam String itemName) {
        return R.ok(healthService.trends(memberId, familyMemberId, itemName));
    }

    @GetMapping("/search")
    public R<List<HealthReportVO>> search(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                          @RequestParam String keyword,
                                          @RequestParam(required = false) Long familyMemberId) {
        return R.ok(healthService.search(memberId, keyword, familyMemberId));
    }

    // ==================== 就医记录 ====================

    @PostMapping("/visits")
    public R<VisitVO> createVisit(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                  @Valid @RequestBody VisitCreateDTO dto) {
        return R.ok(healthService.createVisit(memberId, dto));
    }

    @GetMapping("/visits")
    public R<PageVO<VisitVO>> listVisits(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                         @RequestParam(required = false) Long familyMemberId,
                                         @RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size) {
        return R.ok(healthService.listVisits(memberId, familyMemberId, page, size));
    }

    @DeleteMapping("/visits/{id}")
    public R<Void> deleteVisit(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                               @PathVariable Long id) {
        healthService.deleteVisit(memberId, id);
        return R.ok();
    }

    // ==================== 日常指标 ====================

    @PostMapping("/metrics")
    public R<List<MetricVO>> addMetrics(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                        @Valid @RequestBody MetricBatchDTO dto) {
        return R.ok(healthService.addMetrics(memberId, dto));
    }

    @GetMapping("/metrics")
    public R<PageVO<MetricVO>> listMetrics(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                           @RequestParam(required = false) Long familyMemberId,
                                           @RequestParam(required = false) String metricType,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long size) {
        return R.ok(healthService.listMetrics(memberId, familyMemberId, metricType, page, size));
    }

    @DeleteMapping("/metrics/{id}")
    public R<Void> deleteMetric(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @PathVariable Long id) {
        healthService.deleteMetric(memberId, id);
        return R.ok();
    }

    // ==================== 孕期助手 ====================

    @PostMapping("/pregnancy")
    public R<PregnancyVO> upsertPregnancy(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                          @Valid @RequestBody PregnancyUpsertDTO dto) {
        return R.ok(healthService.upsertPregnancy(memberId, dto));
    }

    @GetMapping("/pregnancy")
    public R<PregnancyVO> getPregnancy(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                       @RequestParam Long familyMemberId) {
        return R.ok(healthService.getPregnancy(memberId, familyMemberId));
    }

    /** 日常指标趋势（孕期体重曲线等） */
    @GetMapping("/metrics/trends")
    public R<List<com.xp.starsite.health.vo.MetricTrendPoint>> metricTrends(
            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
            @RequestParam Long familyMemberId,
            @RequestParam String metricType) {
        return R.ok(healthService.metricTrends(memberId, familyMemberId, metricType));
    }
}
