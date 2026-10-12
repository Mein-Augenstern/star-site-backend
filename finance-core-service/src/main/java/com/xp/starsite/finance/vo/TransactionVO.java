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

    private Long id;

    private Long accountId;

    private String accountName;

    /** 1收入 2支出 3转账 4调整 */
    private Integer direction;

    private BigDecimal amount;

    private String category;

    private String categoryName;

    private LocalDateTime transTime;

    private String counterparty;

    private Long transferGroupId;

    /** 记账后余额快照 */
    private BigDecimal balanceAfter;

    private Integer source;

    private String remark;

    private LocalDateTime createdAt;
}
