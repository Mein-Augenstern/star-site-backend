package com.xp.starsite.finance.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 财务诊断结果（跨语言契约）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class FinanceDiagnosisDTO implements Serializable {

    /** 诊断建议文本（白话中文） */
    private String text;
}
