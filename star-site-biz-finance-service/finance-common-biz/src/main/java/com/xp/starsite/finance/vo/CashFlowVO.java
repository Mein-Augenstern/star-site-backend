package com.xp.starsite.finance.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 现金流量表（V1 简化口径）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class CashFlowVO {

    /** 流动资产余额合计（cash/bank/wechat/alipay） = 手头能动的钱 */
    private BigDecimal liquidBalance;

    /** 近 12 个月月均支出（无流水时 null） */
    private BigDecimal monthlyAvgExpense;

    /** 可撑月数 = 流动余额 / 月均支出（月均支出为 0 时 null = 无限） */
    private BigDecimal monthsCovered;
}
