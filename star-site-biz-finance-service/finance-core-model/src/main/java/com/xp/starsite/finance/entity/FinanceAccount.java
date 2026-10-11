package com.xp.starsite.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金账户（钱在哪；credit/loan 为负债账户）
 */
@Data
@TableName("finance_account")
public class FinanceAccount {

    /** 雪花 id：流水与账户同域，无跨服务事件需求，但统一 ASSIGN_ID 保持范式一致 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long familyId;

    private Long ownerMemberId;

    private String name;

    /** cash/bank/wechat/alipay/broker/fund/credit/loan */
    private String accountType;

    /** 当前余额（流水增量维护；负债账户余额为尚欠金额） */
    private BigDecimal balance;

    /** 期初余额（建户时的钱） */
    private BigDecimal initialBalance;

    /** 0资产账户 1负债账户 */
    private Integer liabilityFlag;

    private String institution;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
