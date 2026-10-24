package com.xp.starsite.file.dal.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件记录数据对象（file_record 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/FileRecord。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class FileRecordDO {

    /** 主键 */
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** 存储键（本地路径或 OSS key） */
    private String storageKey;

    /** 内容类型 */
    private String contentType;

    /** 文件大小（字节） */
    private Long sizeBytes;

    /** 业务类型（avatar/report/bill_import/other） */
    private String bizType;

    /** 上传者会员 id */
    private Long uploaderMemberId;

    /** 创建时间（DB 默认值维护） */
    private LocalDateTime createdAt;
}
