package com.xp.starsite.health.ai;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.llm.LlmClient;
import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.health.entity.HealthIndicator;
import com.xp.starsite.health.entity.HealthReport;
import com.xp.starsite.health.mapper.HealthIndicatorMapper;
import com.xp.starsite.health.mapper.HealthReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 体检报告 AI 解读：聚合报告 + 指标 → LLM → 结果落库缓存（同报告不重复花钱，force 强制重解读）。
 * LlmClient 可选：未配 llm.api-key 时抛 400"未启用"提示；LLM 调用失败抛 503。
 * 越权防线：复用 getDetail 聚合（内部已做归属校验），此处按 id+owner 查询即安全。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportInterpretationService {

    private static final String SYSTEM_PROMPT = """
            你是家庭健康助手。请用通俗易懂的中文解读这份体检报告：
            1. 先总体评价健康状况；
            2. 逐条指出异常指标（偏高/偏低）及其常见含义；
            3. 给出饮食、运动、复查方面的建议；
            4. 最后必须声明：以上分析仅供参考，不构成医疗诊断，异常指标请遵医嘱就诊。
            全文 300 字以内，不要使用 Markdown 标记。""";

    private final HealthReportMapper reportMapper;
    private final HealthIndicatorMapper indicatorMapper;
    private final FamilyOwnershipGuard ownershipGuard;
    private final ObjectProvider<LlmClient> llmClient;

    /**
     * 解读（结果落库）。数据归属由调用方校验（Controller 层先查报告归属）。
     *
     * @param force true = 已解读过也重新调 LLM
     */
    public InterpretationResult interpret(Long memberId, Long reportId, boolean force) {
        HealthReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "报告不存在");
        }
        // 越权防线：报告归属的家庭成员必须属于当前账号
        ownershipGuard.requireOwned(memberId, report.getFamilyMemberId());
        if (!force && report.getInterpretation() != null && !report.getInterpretation().isBlank()) {
            return new InterpretationResult(report.getInterpretation(),
                    report.getInterpretedAt() == null ? null : report.getInterpretedAt().toString(), true);
        }
        List<HealthIndicator> indicators = indicatorMapper.selectByReportId(reportId);

        LlmClient client = llmClient.getIfAvailable();
        if (client == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "AI 解读未启用（未配置 LLM_API_KEY）");
        }

        StringBuilder user = new StringBuilder();
        user.append("报告：").append(report.getReportYear()).append("年体检");
        if (report.getHospital() != null && !report.getHospital().isBlank()) {
            user.append("（").append(report.getHospital()).append("）");
        }
        user.append("，共 ").append(indicators.size()).append(" 项指标：\n");
        for (HealthIndicator i : indicators) {
            String value = i.getValueNum() != null ? i.getValueNum().toPlainString()
                    : (i.getValueText() == null ? "-" : i.getValueText());
            user.append("- ").append(i.getItemName()).append(": ").append(value);
            if (i.getUnit() != null && !i.getUnit().isBlank()) {
                user.append(i.getUnit());
            }
            if (i.getRefLow() != null || i.getRefHigh() != null) {
                user.append("（参考 ").append(i.getRefLow() == null ? "-" : i.getRefLow().toPlainString())
                        .append("~").append(i.getRefHigh() == null ? "-" : i.getRefHigh().toPlainString()).append("）");
            }
            if (i.getAbnormalFlag() != null && i.getAbnormalFlag() > 0) {
                user.append(i.getAbnormalFlag() == 1 ? "【偏高】" : "【偏低】");
            }
            user.append("\n");
        }

        String text;
        try {
            text = client.chat(SYSTEM_PROMPT, user.toString());
        } catch (LlmClient.LlmException e) {
            throw new BizException(503, "AI 服务暂不可用，请稍后再试");
        }

        reportMapper.updateInterpretation(reportId, text, LocalDateTime.now());
        log.info("AI 解读完成: report={}, len={}", reportId, text.length());
        return new InterpretationResult(text, report.getInterpretedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), false);
    }

    /** 解读结果 */
    public record InterpretationResult(String text, String interpretedAt, boolean cached) {
    }
}
