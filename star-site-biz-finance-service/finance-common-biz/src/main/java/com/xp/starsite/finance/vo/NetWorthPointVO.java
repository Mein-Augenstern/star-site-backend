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

    private LocalDate date;

    private BigDecimal netWorth;
}
