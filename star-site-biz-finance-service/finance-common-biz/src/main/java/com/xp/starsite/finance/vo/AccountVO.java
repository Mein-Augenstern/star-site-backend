package com.xp.starsite.finance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户视图
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class AccountVO {

    /** 账户 id */
    private Long id;

    /** 账户名（如 工资卡/微信零钱） */
    private String name;

    /** 账户类型：cash/bank/wechat/alipay/broker/fund/credit/loan */
    private String accountType;

    /** 当前余额（元；负债账户为尚欠金额） */
    private BigDecimal balance;

    /** 期初余额（元） */
    private BigDecimal initialBalance;

    /** 0资产账户 1负债账户 */
    private Integer liabilityFlag;

    /** 开户机构（银行/券商名） */
    private String institution;

    /** 备注 */
    private String remark;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
