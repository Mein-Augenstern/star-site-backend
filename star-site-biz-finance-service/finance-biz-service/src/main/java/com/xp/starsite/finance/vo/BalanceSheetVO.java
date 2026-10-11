package com.xp.starsite.finance.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资产负债表
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class BalanceSheetVO {

    /** 财务账户资产合计（现金/存款/理财等非负债账户余额） */
    private BigDecimal accountAsset;

    /** 档案资产估值合计（archive 域：房产/车辆等；archive 不可用时为 null 并标注） */
    private BigDecimal archiveAsset;

    private Boolean archiveAvailable;

    private BigDecimal totalAsset;

    /** 负债账户余额合计（信用卡/贷款尚欠） */
    private BigDecimal totalLiability;

    /** 净资产 = 总资产 - 总负债 */
    private BigDecimal netWorth;
}
