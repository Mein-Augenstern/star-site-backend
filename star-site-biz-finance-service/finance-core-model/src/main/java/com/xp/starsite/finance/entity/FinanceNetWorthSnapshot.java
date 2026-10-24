package com.xp.starsite.finance.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 净资产快照（finance_net_worth_snapshot）：每日调度落一行，
 * uk(owner_member_id, snap_date) 幂等；净资产曲线（F2 资产负债表）的数据源。
 *
 * @author muyi
 * @since 2026-10
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 */
@Data
public class FinanceNetWorthSnapshot {

    /** 主键 */
    private Long id;

    /** 家庭 id（多人协作预留冗余） */
    private Long familyId;

    /** 归属账号 id（member 服务） */
    private Long ownerMemberId;

    /** 快照日期 */
    private LocalDate snapDate;

    /** 当日资产总额（资产账户余额合计） */
    private BigDecimal totalAsset;

    /** 当日负债总额（负债账户尚欠合计） */
    private BigDecimal totalLiability;

    /** 净资产 = 资产 - 负债 */
    private BigDecimal netWorth;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
