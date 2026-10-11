package com.xp.starsite.finance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户视图
 */
@Data
public class AccountVO {

    private Long id;

    private String name;

    private String accountType;

    private BigDecimal balance;

    private BigDecimal initialBalance;

    private Integer liabilityFlag;

    private String institution;

    private String remark;

    private LocalDateTime createdAt;
}
