package com.xp.starsite.health.integration;

import com.xp.starsite.health.integration.ai.AiApi;
import com.xp.starsite.health.integration.ai.dto.ImageOcrRequestDTO;
import com.xp.starsite.health.integration.ai.dto.OcrDraftDTO;
import com.xp.starsite.health.integration.ai.dto.ReportInterpretDTO;
import com.xp.starsite.health.integration.ai.dto.ReportInterpretRequestDTO;
import com.xp.starsite.common.rpc.RpcIntegration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 智能体服务集成层（SOFA integration 层）：health 域对 AI 能力的唯一出口。
 * 领域内其他类禁止直接依赖 AI 服务的 Feign。
 * AI 是解读/OCR 的强依赖，fail-closed：失败由 RpcClientAspect 统一翻译为 RpcException 上抛。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RpcIntegration("star-site-platform-ai")
@RequiredArgsConstructor
public class AiIntegration {

    private final AiApi aiApi;

    /**
     * 拍照 OCR：图片 data URL → 结构化草稿
     *
     * @throws com.xp.starsite.common.rpc.RpcException AI 服务不可用/识别失败（语义见响应码）
     */
    public OcrDraftDTO reportOcr(String imageDataUrl) {
        ImageOcrRequestDTO request = new ImageOcrRequestDTO();
        request.setImageDataUrl(imageDataUrl);
        return aiApi.reportOcr(request).getData();
    }

    /**
     * 报告解读：结构化报告 → 白话解读文本
     *
     * @throws com.xp.starsite.common.rpc.RpcException AI 服务不可用/解读失败
     */
    public ReportInterpretDTO reportInterpret(ReportInterpretRequestDTO request) {
        return aiApi.reportInterpret(request).getData();
    }
}
