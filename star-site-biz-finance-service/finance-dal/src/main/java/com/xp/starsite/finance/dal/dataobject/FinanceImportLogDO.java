package com.xp.starsite.finance.dal.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账单导入记录数据对象（finance_import_log 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/FinanceImportLog。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class FinanceImportLogDO {

    /** 主键 */
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
