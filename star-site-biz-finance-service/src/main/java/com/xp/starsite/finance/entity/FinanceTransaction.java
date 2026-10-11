package com.xp.starsite.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易流水（唯一事实源；余额快照 balance_after 用于对账）
 */
@Data
@TableName("finance_transaction")
public class FinanceTransaction {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long familyId;

    private Long ownerMemberId;

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

    private LocalDateTime transTime;

    private String counterparty;

    /** 转账组 id（出+入两笔共享） */
    private Long transferGroupId;

    /** 记账后余额快照（对账用） */
    private BigDecimal balanceAfter;

    /** 1手记 2微信导入 3支付宝导入 */
    private Integer source;

    /** 账单方交易号（与 source 组成唯一键，导入幂等；手记为 null） */
    private String externalId;

    private Long fileId;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
