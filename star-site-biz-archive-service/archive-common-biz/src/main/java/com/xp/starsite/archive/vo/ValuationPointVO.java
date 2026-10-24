package com.xp.starsite.archive.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 估值曲线点
 *
 * @author muyi
 * @since 2026-10
 */
@Data
@AllArgsConstructor
public class ValuationPointVO {

    /** 该时点全家资产估值合计（元） */
    private BigDecimal value;

    /** 估值时间 */
    private LocalDateTime valuedAt;
}
