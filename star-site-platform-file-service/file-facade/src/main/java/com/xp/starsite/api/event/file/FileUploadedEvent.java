package com.xp.starsite.api.event.file;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 文件上传完成事件（TOPIC_FILE）。上传成功后由 file 服务发出，
 * 消费方按 bizType 决定是否处理（账单导入：bill_import）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class FileUploadedEvent implements Serializable {

    /** attributes 里的导入目标账户 key（bill_import 场景；与上传方表单字段同名约定） */
    public static final String ATTR_ACCOUNT_ID = "accountId";

    /** 文件 id（file 服务） */
    private Long fileId;

    /** 业务类型（avatar/report/bill_import...） */
    private String bizType;

    /** 上传者账号 id */
    private Long uploaderMemberId;

    /** 原始文件名 */
    private String fileName;

    /** 业务元数据（bill_import: accountId=导入目标账户；其余场景可空） */
    private Map<String, String> attributes;
}
