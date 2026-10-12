package com.xp.starsite.archive.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 估值曲线点
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@AllArgsConstructor
public class ValuationPointVO {

    private BigDecimal value;

    private LocalDateTime valuedAt;
}
