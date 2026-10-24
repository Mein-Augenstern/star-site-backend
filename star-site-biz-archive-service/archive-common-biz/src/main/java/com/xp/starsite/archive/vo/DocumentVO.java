package com.xp.starsite.archive.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 证件视图
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class DocumentVO {

    /** 证件 id */
    private Long id;

    /** 证件持有人（family 服务自然人档案） */
    private Long holderMemberId;

    /** 持有人姓名快照 */
    private String holderMemberName;

    /** 证件类型：id_card/hukou/property_cert/vehicle_license/diploma/marriage/birth/passport/contract/other */
    private String docType;

    /** 证件号码 */
    private String docNo;

    /** 签发日期 */
    private LocalDate issueDate;

    /** 有效期至，null = 长期（到期提醒源） */
    private LocalDate expiryDate;

    /** 扫描件附件（file 服务） */
    private Long fileId;

    /** 备注 */
    private String remark;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
