package com.xp.starsite.archive.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 估值曲线点
 */
@Data
@AllArgsConstructor
public class ValuationPointVO {

    private BigDecimal value;

    private LocalDateTime valuedAt;
}
