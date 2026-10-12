package com.xp.starsite.file.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件记录（file 表）：上传元数据（原始文件名/存储键/类型/大小），二进制在 FileStorage 侧。
 * created_at 由数据库默认值维护；表无 deleted 列（文件不做逻辑删）。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class FileRecord {

    /** 主键 */
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** 存储键（本地相对路径或 OSS key） */
    private String storageKey;

    /** MIME 类型 */
    private String contentType;

    /** 字节数 */
    private Long sizeBytes;

    /** avatar/report/other */
    private String bizType;

    /** 上传者账号 id */
    private Long uploaderMemberId;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
