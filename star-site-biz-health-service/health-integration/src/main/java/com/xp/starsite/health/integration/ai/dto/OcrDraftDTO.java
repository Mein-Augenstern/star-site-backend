package com.xp.starsite.health.integration.ai.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * OCR 结构化草稿（跨语言契约，字段与 Python app/schemas.py OcrDraft 对齐）。
 * 消费方映射到 health 自己的 OcrDraft（IndicatorDTO 列表）预填表单。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class OcrDraftDTO implements Serializable {

    /** 报告标题（识别不出为 null） */
    private String title;

    /** 医院（识别不出为 null） */
    private String hospital;

    /** 体检日期（YYYY-MM-DD 或 null） */
    private String checkupDate;

    /** 报告年份 */
    private Integer reportYear;

    /** 指标草稿列表 */
    private List<OcrIndicatorDTO> indicators;
}
