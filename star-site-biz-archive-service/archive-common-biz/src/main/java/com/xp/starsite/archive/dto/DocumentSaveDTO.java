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

    /** 证件类型：id_card/hukou/property_cert/vehicle_license/diploma/marriage/birth/passport/contract/other */
    @NotBlank(message = "证件类型不能为空")
    private String docType;

    /** 证件持有人（family 服务自然人档案） */
    @NotNull(message = "持有人不能为空")
    private Long holderMemberId;

    /** 证件号码 */
    private String docNo;

    /** 签发日期 */
    private LocalDate issueDate;

    /** 有效期至，空 = 长期 */
    private LocalDate expiryDate;

    /** 扫描件附件（file 服务） */
    private Long fileId;

    /** 备注 */
    private String remark;
}
