package com.xp.starsite.finance.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 净资产快照数据对象（finance_net_worth_snapshot 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/FinanceNetWorthSnapshot。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class FinanceNetWorthSnapshotDO {

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
