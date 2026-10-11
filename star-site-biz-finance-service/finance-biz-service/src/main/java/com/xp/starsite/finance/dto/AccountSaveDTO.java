package com.xp.starsite.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户创建/编辑入参
 */
@Data
public class AccountSaveDTO {

    @NotBlank(message = "账户名称不能为空")
    private String name;

    @NotBlank(message = "账户类型不能为空")
    private String accountType;

    /** 期初余额（建户时的钱） */
    private BigDecimal initialBalance;

    private String institution;

    private String remark;
}
