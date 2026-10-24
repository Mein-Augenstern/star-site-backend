package com.xp.starsite.health.ai;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.health.entity.HealthIndicator;
import com.xp.starsite.health.entity.HealthReport;
import com.xp.starsite.health.integration.AiIntegration;
import com.xp.starsite.health.integration.ai.dto.InterpretIndicatorDTO;
import com.xp.starsite.health.integration.ai.dto.ReportInterpretDTO;
import com.xp.starsite.health.integration.ai.dto.ReportInterpretRequestDTO;
import com.xp.starsite.health.repository.HealthIndicatorRepository;
import com.xp.starsite.health.repository.HealthReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 体检报告 AI 解读：聚合报告 + 指标 → 结构化数据发 AI 智能体服务（prompt/模型在 AI 侧）→
 * 结果落库缓存（同报告不重复花钱，force 强制重解读）。
 * AI 服务不可用时抛 503（fail-closed 翻译自 RpcException）；
 * 越权防线：复用 getDetail 聚合（内部已做归属校验），此处按 id+owner 查询即安全。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportInterpretationService {

    private final HealthReportRepository reportRepository;
    private final HealthIndicatorRepository indicatorRepository;
    private final FamilyOwnershipGuard ownershipGuard;
    private final AiIntegration aiIntegration;

    /**
     * 解读（结果落库）。数据归属由调用方校验（Controller 层先查报告归属）。
     *
     * @param force true = 已解读过也重新调 AI
     */
    public InterpretationResult interpret(Long memberId, Long reportId, boolean force) {
        HealthReport report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "报告不存在");
        }
        // 越权防线：报告归属的家庭成员必须属于当前账号
        ownershipGuard.requireOwned(memberId, report.getFamilyMemberId());
        if (!force && report.getInterpretation() != null && !report.getInterpretation().isBlank()) {
            return new InterpretationResult(report.getInterpretation(),
                    report.getInterpretedAt() == null ? null : report.getInterpretedAt().toString(), true);
        }
        List<HealthIndicator> indicators = indicatorRepository.findByReportId(reportId);

        ReportInterpretDTO result;
        try {
            result = aiIntegration.reportInterpret(toRequest(report, indicators));
        } catch (com.xp.starsite.common.rpc.RpcException e) {
            throw new BizException(503, "AI 服务暂不可用，请稍后再试");
        }

        String text = result.getText();
        reportRepository.updateInterpretation(reportId, text, LocalDateTime.now());
        log.info("AI 解读完成: report={}, len={}", reportId, text.length());
        return new InterpretationResult(text, report.getInterpretedAt() == null
                ? LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : report.getInterpretedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), false);
    }

    /** 领域实体 → 结构化解读请求（渲染在 AI 服务侧） */
    private ReportInterpretRequestDTO toRequest(HealthReport report, List<HealthIndicator> indicators) {
        ReportInterpretRequestDTO request = new ReportInterpretRequestDTO();
        request.setReportYear(report.getReportYear());
        request.setHospital(report.getHospital());
        List<InterpretIndicatorDTO> rows = new ArrayList<>();
        for (HealthIndicator i : indicators) {
            InterpretIndicatorDTO row = new InterpretIndicatorDTO();
            row.setName(i.getItemName());
            String value = i.getValueNum() != null ? i.getValueNum().toPlainString()
                    : (i.getValueText() == null ? "-" : i.getValueText());
            row.setValue(value);
            row.setUnit(i.getUnit());
            row.setRefLow(i.getRefLow() == null ? null : i.getRefLow().toPlainString());
            row.setRefHigh(i.getRefHigh() == null ? null : i.getRefHigh().toPlainString());
            row.setAbnormalFlag(i.getAbnormalFlag() == null ? 0 : i.getAbnormalFlag());
            rows.add(row);
        }
        request.setIndicators(rows);
        return request;
    }

    /** 解读结果 */
    public record InterpretationResult(String text, String interpretedAt, boolean cached) {
    }
}
