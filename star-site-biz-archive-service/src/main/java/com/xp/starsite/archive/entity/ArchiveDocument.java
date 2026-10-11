package com.xp.starsite.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭证件
 */
@Data
@TableName("archive_document")
public class ArchiveDocument {

    /** 雪花 id（应用侧赋值）：到期提醒事件需在 insert 前拿到 id 填进事件载荷 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long familyId;

    private Long ownerMemberId;

    /** 证件持有人（family 服务自然人档案） */
    private Long holderMemberId;

    /** 持有人姓名快照 */
    private String holderMemberName;

    /** id_card/hukou/property_cert/vehicle_license/diploma/marriage/birth/passport/contract/other */
    private String docType;

    private String docNo;

    private LocalDate issueDate;

    /** 有效期至，null = 长期（提醒源） */
    private LocalDate expiryDate;

    /** 扫描件附件（file 服务） */
    private Long fileId;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
