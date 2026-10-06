package com.xp.starsite.file.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 上传出参
 */
@Data
@Builder
public class FileVO {

    private Long fileId;

    private String name;

    private String contentType;

    private Long size;
}
