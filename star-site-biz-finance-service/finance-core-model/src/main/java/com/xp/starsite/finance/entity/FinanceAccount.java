package com.xp.starsite.finance.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金账户（finance_account）：钱在哪；credit/loan 为负债账户（余额=尚欠金额）。
 * 余额由流水增量维护，期初余额建户时一次性录入。
 *
 * @author xiaopu
 * @since 2026-10
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 */
@Data
public class FinanceAccount {

    /** 雪花 id（应用侧赋值）：与其他 finance 表范式一致 */
    private Long id;

    /** 家庭 id（多人协作预留冗余） */
    private Long familyId;

    /** 归属账号 id（member 服务） */
    private Long ownerMemberId;

    /** 账户名（如 工资卡/微信零钱） */
    private String name;

    /** 账户类型：cash/bank/wechat/alipay/broker/fund/credit/loan */
    private String accountType;

    /** 当前余额（流水增量维护；负债账户余额为尚欠金额） */
    private BigDecimal balance;

    /** 期初余额（建户时的钱） */
    private BigDecimal initialBalance;

    /** 0资产账户 1负债账户 */
    private Integer liabilityFlag;

    /** 开户机构（银行/券商名） */
    private String institution;

    /** 备注 */
    private String remark;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
