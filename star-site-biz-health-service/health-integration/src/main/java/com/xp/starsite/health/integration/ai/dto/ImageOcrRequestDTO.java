package com.xp.starsite.health.integration.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 拍照 OCR 请求（跨语言契约，字段与 Python app/schemas.py OcrRequest 对齐）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class ImageOcrRequestDTO implements Serializable {

    /** 报告图片 data URL（data:image/xxx;base64,...） */
    private String imageDataUrl;
}
