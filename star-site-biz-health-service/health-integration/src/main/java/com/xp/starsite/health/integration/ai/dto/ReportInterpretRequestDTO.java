package com.xp.starsite.health.integration.ai.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 报告解读请求（跨语言契约，字段与 Python InterpretRequest 对齐；prompt 在 AI 服务侧）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class ReportInterpretRequestDTO implements Serializable {

    /** 报告年份 */
    private Integer reportYear;

    /** 医院，可空 */
    private String hospital;

    /** 指标全集（结构化行，AI 服务自行渲染成模型输入） */
    private List<InterpretIndicatorDTO> indicators;
}
