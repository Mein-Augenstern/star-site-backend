package com.xp.starsite.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 净资产快照（每日调度落一行，uk(owner, snap_date) 幂等）
 */
@Data
@TableName("finance_net_worth_snapshot")
public class FinanceNetWorthSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyId;

    private Long ownerMemberId;

    private LocalDate snapDate;

    private BigDecimal totalAsset;

    private BigDecimal totalLiability;

    private BigDecimal netWorth;

    private LocalDateTime createdAt;
}
