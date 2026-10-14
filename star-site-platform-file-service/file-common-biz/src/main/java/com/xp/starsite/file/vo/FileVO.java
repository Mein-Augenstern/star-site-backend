package com.xp.starsite.file.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 上传出参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@Builder
public class FileVO {

    /** 文件 id（全局唯一，各服务凭它引用/下载附件） */
    private Long fileId;

    /** 原始文件名 */
    private String name;

    /** MIME 类型（如 image/png） */
    private String contentType;

    /** 文件大小（字节） */
    private Long size;
}
