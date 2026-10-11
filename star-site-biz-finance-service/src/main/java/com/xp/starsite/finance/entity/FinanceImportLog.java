package com.xp.starsite.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账单导入记录（FileUploaded 事件消费结果，前端轮询展示）
 */
@Data
@TableName("finance_import_log")
public class FinanceImportLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ownerMemberId;

    private Long accountId;

    private Long fileId;

    private String channel;

    private Integer parsed;

    private Integer imported;

    private Integer duplicates;

    private Integer skipped;

    /** 1成功 2失败 */
    private Integer status;

    private String error;

    private LocalDateTime createdAt;
}
