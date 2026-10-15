package com.xp.starsite.health.ai;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.health.dto.IndicatorDTO;
import com.xp.starsite.health.integration.AiIntegration;
import com.xp.starsite.health.integration.FileIntegration;
import com.xp.starsite.health.integration.ai.dto.OcrDraftDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 报告拍照识别：图片 → AI 智能体服务（prompt/模型/解析集中在 AI 服务侧）→ 结构化草稿
 * （预填表单，人工确认后落库）。
 * 设计原则：识别结果只做"草稿"不直接入库，错误由用户在表单里修正——
 * 降低录入成本的同时保住数据可信度。文件字节经 FileIntegration 读取，
 * 识别经 AiIntegration（fail-closed，失败翻译为友好提示）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportOcrService {

    private final FileIntegration fileIntegration;
    private final AiIntegration aiIntegration;

    public OcrDraft ocr(Long fileId) {
        byte[] bytes = fileIntegration.content(fileId);
        if (bytes == null || bytes.length == 0) {
            throw new BizException(400, "文件不存在或已删除");
        }
        String mime = detectMime(bytes);
        if (!mime.startsWith("image/")) {
            // PDF 等非图片：GLM-4V 只吃图片；明确引导而非报一堆乱码
            throw new BizException(400, "AI 识别仅支持图片（拍照/截图）；PDF 报告请直接归档，需要识别时可截图后拍照识别");
        }
        String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        OcrDraftDTO dto;
        try {
            dto = aiIntegration.reportOcr(dataUrl);
        } catch (com.xp.starsite.common.rpc.RpcException e) {
            // 400=模型输出解析失败；其余=AI 服务不可用。都转友好提示，本地状态未动
            if (e.getCode() == 400) {
                throw new BizException(503, "识别结果解析失败，请手动录入");
            }
            log.warn("AI 识别调用失败: code={}, retryable={}", e.getCode(), e.isRetryable());
            throw new BizException(503, "识别服务暂不可用，请稍后再试或手动录入");
        }
        return toDraft(dto);
    }

    /** facade DTO → 预填草稿（保留旧版容错：空指标名跳过、缺年份落当前年） */
    private OcrDraft toDraft(OcrDraftDTO dto) {
        OcrDraft draft = new OcrDraft();
        draft.setTitle(dto.getTitle());
        draft.setHospital(dto.getHospital());
        draft.setCheckupDate(dto.getCheckupDate());
        draft.setReportYear(dto.getReportYear() != null ? dto.getReportYear()
                : java.time.LocalDate.now().getYear());
        List<IndicatorDTO> items = new ArrayList<>();
        if (dto.getIndicators() != null) {
            for (var i : dto.getIndicators()) {
                if (i.getItemName() == null || i.getItemName().isBlank()) {
                    continue;
                }
                IndicatorDTO indicator = new IndicatorDTO();
                indicator.setItemName(i.getItemName());
                indicator.setItemGroup(i.getItemGroup());
                indicator.setValueNum(i.getValueNum());
                indicator.setValueText(i.getValueText());
                indicator.setUnit(i.getUnit());
                indicator.setRefLow(i.getRefLow());
                indicator.setRefHigh(i.getRefHigh());
                items.add(indicator);
            }
        }
        draft.setIndicators(items);
        log.info("OCR 草稿: 指标 {} 项, 医院={}", items.size(), draft.getHospital());
        return draft;
    }

    /** 魔数识别真实类型：PNG/JPG/WEBP/PDF，防扩展名造假 */
    private String detectMime(byte[] b) {
        if (b.length >= 4 && (b[0] & 0xff) == 0x89 && b[1] == 'P') return "image/png";
        if (b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8) return "image/jpeg";
        if (b.length >= 12 && new String(b, 0, 4).equals("RIFF") && new String(b, 8, 4).equals("WEBP")) return "image/webp";
        if (b.length >= 4 && new String(b, 0, 4).equals("%PDF")) return "application/pdf";
        return "image/jpeg";
    }

    /** 识别草稿（预填用，未落库） */
    @lombok.Data
    public static class OcrDraft {
        private String title;
        private String hospital;
        private String checkupDate;
        private Integer reportYear;
        private List<IndicatorDTO> indicators;
    }
}
