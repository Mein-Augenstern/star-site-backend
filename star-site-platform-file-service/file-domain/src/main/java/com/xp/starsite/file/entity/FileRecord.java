package com.xp.starsite.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件记录
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@TableName("file")
public class FileRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** 存储键（本地相对路径或 OSS key） */
    private String storageKey;

    private String contentType;

    private Long sizeBytes;

    /** avatar/report/other */
    private String bizType;

    /** 上传者账号 id */
    private Long uploaderMemberId;

    private LocalDateTime createdAt;
}
