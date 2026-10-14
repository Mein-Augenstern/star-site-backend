package com.xp.starsite.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户创建/编辑入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class AccountSaveDTO {

    @NotBlank(message = "账户名称不能为空")
    /** 账户名（如 工资卡/微信零钱） */
    private String name;

    @NotBlank(message = "账户类型不能为空")
    /** 账户类型：cash/bank/wechat/alipay/broker/fund/credit/loan */
    private String accountType;

    /** 期初余额（建户时的钱） */
    private BigDecimal initialBalance;

    /** 开户机构（银行/券商名），可空 */
    private String institution;

    /** 备注 */
    private String remark;
}
