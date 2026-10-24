package com.xp.starsite.finance.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易流水数据对象（finance_transaction 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/FinanceTransaction。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class FinanceTransactionDO {

    /** 雪花 id（应用侧赋值） */
    private Long id;

    /** 家庭 id（多人协作预留冗余） */
    private Long familyId;

    /** 归属账号 id（member 服务） */
    private Long ownerMemberId;

    /** 资金账户 id（finance_account.id） */
    private Long accountId;

    /** 账户名快照（免联表） */
    private String accountName;

    /** 1收入 2支出 3转账 4调整(余额校准,不计收支) */
    private Integer direction;

    /** 金额（direction=4 调整时可为负，其余恒正） */
    private BigDecimal amount;

    /** 分类枚举（调整/转账可空） */
    private String category;

    /** 分类名快照 */
    private String categoryName;

    /** 交易时间 */
    private LocalDateTime transTime;

    /** 交易对手方（商户/个人） */
    private String counterparty;

    /** 转账组 id（出+入两笔共享，成对查询用） */
    private Long transferGroupId;

    /** 记账后余额快照（对账用） */
    private BigDecimal balanceAfter;

    /** 1手记 2微信导入 3支付宝导入 */
    private Integer source;

    /** 账单方交易号（与 source 组成唯一键，导入幂等；手记为 null） */
    private String externalId;

    /** 凭证附件（file 服务，可空） */
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
