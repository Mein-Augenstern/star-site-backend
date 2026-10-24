package com.xp.starsite.health.integration.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 报告解读结果（跨语言契约）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class ReportInterpretDTO implements Serializable {

    /** 解读文本（白话中文） */
    private String text;
}
