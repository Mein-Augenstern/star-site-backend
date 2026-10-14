package com.xp.starsite.finance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 流水视图
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class TransactionVO {

    /** 流水 id */
    private Long id;

    /** 资金账户 id（finance_account.id） */
    private Long accountId;

    /** 账户名快照（免联表） */
    private String accountName;

    /** 1收入 2支出 3转账 4调整 */
    private Integer direction;

    /** 金额（元；调整方向可为负） */
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

    /** 记账后余额快照 */
    private BigDecimal balanceAfter;

    /** 1手记 2微信导入 3支付宝导入 */
    private Integer source;

    /** 备注 */
    private String remark;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
