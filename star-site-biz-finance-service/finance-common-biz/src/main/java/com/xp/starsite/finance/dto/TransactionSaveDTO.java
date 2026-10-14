package com.xp.starsite.finance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 记账入参
 * mode 由 direction 决定：1 收入 / 2 支出（单账户）；3 转账（两账户）；4 调整（差额校准）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class TransactionSaveDTO {

    @NotNull(message = "记账类型不能为空")
    /** 1收入 2支出 3转账 4调整(余额校准,不计收支) */
    private Integer direction;

    /** 账户（收入/支出/调整 = 本账户；转账 = 转出账户） */
    @NotNull(message = "账户不能为空")
    private Long accountId;

    /** 转入账户（direction=3 必填，须不同于转出账户） */
    private Long toAccountId;

    @NotNull(message = "金额不能为空")
    @Digits(integer = 14, fraction = 2, message = "金额最多两位小数")
    /** 金额（元） */
    private BigDecimal amount;

    /** 分类枚举（调整/转账可空） */
    private String category;

    @NotNull(message = "交易时间不能为空")
    /** 交易时间 */
    private LocalDateTime transTime;

    /** 交易对手方（商户/个人），可空 */
    private String counterparty;

    /** 关联账单文件 id（导入来源），可空 */
    private Long fileId;

    /** 备注 */
    private String remark;
}
