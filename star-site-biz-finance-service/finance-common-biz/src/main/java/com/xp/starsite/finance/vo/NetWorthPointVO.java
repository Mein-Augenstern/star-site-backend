package com.xp.starsite.finance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 净资产曲线点
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class NetWorthPointVO {

    /** 快照日期 */
    private LocalDate date;

    /** 净资产（元） */
    private BigDecimal netWorth;
}
