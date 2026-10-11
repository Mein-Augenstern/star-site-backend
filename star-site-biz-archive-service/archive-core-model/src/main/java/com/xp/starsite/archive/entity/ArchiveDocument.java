package com.xp.starsite.archive.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭证件（archive_document）：证件夹主体表。
 * 到期提醒以 expiry_date 为源（非长期证件），扫描件存 file 服务。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class ArchiveDocument {

    /** 雪花 id（应用侧赋值）：到期提醒事件需在 insert 前拿到 id 填进事件载荷 */
    private Long id;

    /** 家庭 id（多人协作预留冗余） */
    private Long familyId;

    /** 归属账号 id（member 服务） */
    private Long ownerMemberId;

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

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
