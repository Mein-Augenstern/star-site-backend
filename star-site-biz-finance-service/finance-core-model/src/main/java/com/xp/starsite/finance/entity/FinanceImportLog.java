package com.xp.starsite.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账单导入记录（finance_import_log）：FileUploaded 事件消费结果，
 * 前端按 id 轮询导入进度与结果。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@TableName("finance_import_log")
public class FinanceImportLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起导入的账号 id（member 服务） */
    private Long ownerMemberId;

    /** 导入目标账户 id（finance_account.id） */
    private Long accountId;

    /** 账单文件 id（file 服务） */
    private Long fileId;

    /** 账单渠道（wechat/alipay） */
    private String channel;

    /** 解析出的账单行数 */
    private Integer parsed;

    /** 实际入库行数 */
    private Integer imported;

    /** 与既有流水重复（幂等跳过）的行数 */
    private Integer duplicates;

    /** 无法解析而跳过的行数 */
    private Integer skipped;

    /** 1成功 2失败 */
    private Integer status;

    /** 失败原因（成功为 null） */
    private String error;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
