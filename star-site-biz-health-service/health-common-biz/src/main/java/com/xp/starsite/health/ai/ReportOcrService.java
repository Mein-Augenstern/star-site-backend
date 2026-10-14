package com.xp.starsite.health.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.llm.LlmClient;
import com.xp.starsite.health.dto.IndicatorDTO;
import com.xp.starsite.health.integration.FileIntegration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 报告拍照识别：图片 → GLM-4V 多模态 → 结构化草稿（预填表单，人工确认后落库）。
 * 设计原则：识别结果只做"草稿"不直接入库，错误由用户在表单里修正——
 * 降低录入成本的同时保住数据可信度。无 key 时降级提示（与 AI 解读同款）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportOcrService {

    private static final String SYSTEM_PROMPT = """
            你是体检报告结构化助手。从图片中提取体检报告信息，只输出一个 JSON 对象，不要任何其他文字或 Markdown 代码块标记。
            格式：{"title":"标题","hospital":"医院","checkupDate":"YYYY-MM-DD 或 null","reportYear":2026,"indicators":[{"itemName":"指标名","itemGroup":"分组","valueNum":数值或null,"valueText":"文本结果或null","unit":"单位","refLow":下限或null,"refHigh":上限或null}]}
            注意：只提取图片中真实存在的内容，看不清的字段用 null；数值去掉单位和逗号。""";

    private final FileIntegration fileIntegration;
    private final ObjectProvider<LlmClient> llmClient;

    public OcrDraft ocr(Long fileId) {
        LlmClient client = llmClient.getIfAvailable();
        if (client == null) {
            throw new BizException(400, "拍照识别未启用（未配置 LLM_API_KEY），请手动录入");
        }
        byte[] bytes = fileIntegration.content(fileId);
        if (bytes == null || bytes.length == 0) {
            throw new BizException(400, "文件不存在或已删除");
        }
        String mime = detectMime(bytes);
        if (!mime.startsWith("image/")) {
            // PDF 等非图片：GLM-4V 只吃图片；明确引导而非报一堆乱码
            throw new BizException(400, "AI 识别仅支持图片（拍照/截图）；PDF 报告请直接归档，需要识别时可截图后拍照识别");
        }
        String dataUrl = "data:" + mime + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes);
        String text;
        try {
            text = client.chatWithImage(SYSTEM_PROMPT, "请提取这份体检报告的结构化信息。", dataUrl);
        } catch (LlmClient.LlmException e) {
            throw new BizException(503, "识别服务暂不可用，请稍后再试或手动录入");
        }
        return parse(text);
    }

    /** 魔数识别真实类型：PNG/JPG/WEBP/PDF，防扩展名造假 */
    private String detectMime(byte[] b) {
        if (b.length >= 4 && (b[0] & 0xff) == 0x89 && b[1] == 'P') return "image/png";
        if (b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8) return "image/jpeg";
        if (b.length >= 12 && new String(b, 0, 4).equals("RIFF") && new String(b, 8, 4).equals("WEBP")) return "image/webp";
        if (b.length >= 4 && new String(b, 0, 4).equals("%PDF")) return "application/pdf";
        return "image/jpeg";
    }

    /** 宽容解析：剥掉 ```json 围栏、截取首个大括号起的内容 */
    private OcrDraft parse(String text) {
        String json = text.trim();
        int fence = json.indexOf("```");
        if (fence >= 0) {
            int start = json.indexOf('{', fence);
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
        } else {
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            OcrDraft draft = new OcrDraft();
            draft.setTitle(text(root, "title"));
            draft.setHospital(text(root, "hospital"));
            draft.setCheckupDate(text(root, "checkupDate"));
            draft.setReportYear(root.path("reportYear").isInt() ? root.path("reportYear").asInt()
                    : java.time.LocalDate.now().getYear());
            List<IndicatorDTO> items = new ArrayList<>();
            for (JsonNode n : root.path("indicators")) {
                IndicatorDTO dto = new IndicatorDTO();
                dto.setItemName(text(n, "itemName"));
                if (dto.getItemName() == null || dto.getItemName().isBlank()) {
                    continue;
                }
                dto.setItemGroup(text(n, "itemGroup"));
                dto.setValueNum(dec(n, "valueNum"));
                dto.setValueText(text(n, "valueText"));
                dto.setUnit(text(n, "unit"));
                dto.setRefLow(dec(n, "refLow"));
                dto.setRefHigh(dec(n, "refHigh"));
                items.add(dto);
            }
            draft.setIndicators(items);
            log.info("OCR 草稿: 指标 {} 项, 医院={}", items.size(), draft.getHospital());
            return draft;
        } catch (Exception e) {
            log.warn("OCR 结果解析失败: {}", text.substring(0, Math.min(200, text.length())));
            throw new BizException(503, "识别结果解析失败，请手动录入");
        }
    }

    private String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText().trim();
        return s.isEmpty() || "null".equals(s) ? null : s;
    }

    private BigDecimal dec(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull() || !v.isNumber()) {
            return null;
        }
        return v.decimalValue();
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
