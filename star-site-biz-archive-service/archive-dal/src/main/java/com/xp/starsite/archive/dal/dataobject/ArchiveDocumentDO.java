package com.xp.starsite.archive.dal.dataobject;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭证件数据对象（archive_document 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/ArchiveDocument。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class ArchiveDocumentDO {

    /** 主键（雪花 id，应用侧赋值） */
    private Long id;

    /** 家庭 id */
    private Long familyId;

    /** 归属账号 id */
    private Long ownerMemberId;

    /** 持有家庭成员 id */
    private Long holderMemberId;

    /** 持有人姓名快照 */
    private String holderMemberName;

    /** 证件类型（id_card/hukou/passport/...） */
    private String docType;

    /** 证件号码 */
    private String docNo;

    /** 签发日 */
    private LocalDate issueDate;

    /** 有效期至（长期证件为 null） */
    private LocalDate expiryDate;

    /** 证件文件 id（file 服务扫描件） */
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
