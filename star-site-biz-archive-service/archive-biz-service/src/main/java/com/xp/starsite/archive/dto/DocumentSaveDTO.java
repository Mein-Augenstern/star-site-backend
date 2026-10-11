package com.xp.starsite.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 证件创建/编辑入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class DocumentSaveDTO {

    @NotBlank(message = "证件类型不能为空")
    private String docType;

    @NotNull(message = "持有人不能为空")
    private Long holderMemberId;

    private String docNo;

    private LocalDate issueDate;

    /** 有效期至，空 = 长期 */
    private LocalDate expiryDate;

    private Long fileId;

    private String remark;
}
