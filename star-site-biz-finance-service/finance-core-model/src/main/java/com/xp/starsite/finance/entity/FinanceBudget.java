package com.xp.starsite.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度分类预算
 */
@Data
@TableName("finance_budget")
public class FinanceBudget {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long familyId;

    private Long ownerMemberId;

    /** 分类枚举（支出类） */
    private String category;

    /** 预算月份 YYYY-MM */
    private String period;

    private BigDecimal amount;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
