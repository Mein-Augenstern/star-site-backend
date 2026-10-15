package com.xp.starsite.health.integration.ai;

import com.xp.starsite.common.result.R;
import com.xp.starsite.health.integration.ai.dto.ImageOcrRequestDTO;
import com.xp.starsite.health.integration.ai.dto.OcrDraftDTO;
import com.xp.starsite.health.integration.ai.dto.ReportInterpretDTO;
import com.xp.starsite.health.integration.ai.dto.ReportInterpretRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI 智能体服务内部接口（Feign 契约，跨语言：提供方为 Python/FastAPI）。
 * AI 服务是平台唯一的 LLM 出口（prompt/模型/解析集中在对方），health 只发结构化数据。
 * 提供方为非 JVM 服务，契约定义在消费方 integration 模块（docs/技术/12 §跨语言规约）。
 *
 * /internal/** 不经网关路由（外网 404），仅限内网服务间调用。
 *
 * @author xiaopu
 * @since 2026-10
 */
@FeignClient(name = "star-site-platform-ai", contextId = "aiInternalApi", path = "/internal/ai")
public interface AiApi {

    /**
     * 拍照 OCR：图片 → 结构化指标草稿（强类型）
     *
     * @return 识别草稿（title/hospital/checkupDate/reportYear/indicators）
     */
    @PostMapping("/report-ocr")
    R<OcrDraftDTO> reportOcr(@RequestBody ImageOcrRequestDTO request);

    /**
     * 报告解读：结构化报告 + 指标 → 白话解读文本
     *
     * @return 解读结果（text）
     */
    @PostMapping("/report-interpret")
    R<ReportInterpretDTO> reportInterpret(@RequestBody ReportInterpretRequestDTO request);
}
